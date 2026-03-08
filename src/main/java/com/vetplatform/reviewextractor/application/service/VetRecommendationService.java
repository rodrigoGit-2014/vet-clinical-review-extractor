package com.vetplatform.reviewextractor.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vetplatform.reviewextractor.domain.entity.RecommendationMatch;
import com.vetplatform.reviewextractor.domain.entity.VetRecommendation;
import com.vetplatform.reviewextractor.domain.entity.VetRecommendationRequest;
import com.vetplatform.reviewextractor.domain.enums.RecommendationStatus;
import com.vetplatform.reviewextractor.domain.repository.RecommendationMatchRepository;
import com.vetplatform.reviewextractor.domain.repository.VetRecommendationRepository;
import com.vetplatform.reviewextractor.domain.repository.VetRecommendationRequestRepository;
import com.vetplatform.reviewextractor.dto.request.CreateRecommendationRequest;
import com.vetplatform.reviewextractor.dto.request.RecalculateRecommendationRequest;
import com.vetplatform.reviewextractor.dto.response.CreateRecommendationResponse;
import com.vetplatform.reviewextractor.dto.response.RecommendationResultResponse;
import com.vetplatform.reviewextractor.dto.response.RecommendationStatusResponse;
import com.vetplatform.reviewextractor.dto.response.RecalculateRecommendationResponse;
import com.vetplatform.reviewextractor.infrastructure.exception.RecommendationAlreadyProcessingException;
import com.vetplatform.reviewextractor.infrastructure.exception.RecommendationNotFoundException;
import com.vetplatform.reviewextractor.infrastructure.llm.PromptBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class VetRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(VetRecommendationService.class);

    private final VetRecommendationRequestRepository requestRepository;
    private final VetRecommendationRepository recommendationRepository;
    private final RecommendationMatchRepository matchRepository;
    private final ClientCaseInterpreterService interpreterService;
    private final InterpretationValidatorService validatorService;
    private final ClinicalNormalizationService normalizationService;
    private final CaseMatchingService matchingService;
    private final VetScoringService scoringService;
    private final ProcessingAuditService auditService;
    private final PromptBuilderService promptBuilder;
    private final ObjectMapper objectMapper;

    public VetRecommendationService(
            VetRecommendationRequestRepository requestRepository,
            VetRecommendationRepository recommendationRepository,
            RecommendationMatchRepository matchRepository,
            ClientCaseInterpreterService interpreterService,
            InterpretationValidatorService validatorService,
            ClinicalNormalizationService normalizationService,
            CaseMatchingService matchingService,
            VetScoringService scoringService,
            ProcessingAuditService auditService,
            PromptBuilderService promptBuilder,
            ObjectMapper objectMapper
    ) {
        this.requestRepository = requestRepository;
        this.recommendationRepository = recommendationRepository;
        this.matchRepository = matchRepository;
        this.interpreterService = interpreterService;
        this.validatorService = validatorService;
        this.normalizationService = normalizationService;
        this.matchingService = matchingService;
        this.scoringService = scoringService;
        this.auditService = auditService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateRecommendationResponse createRecommendation(CreateRecommendationRequest request) {
        VetRecommendationRequest reqEntity = new VetRecommendationRequest();
        reqEntity.setClientText(request.clientText());
        reqEntity.setPetOwnerId(request.petOwnerId());
        reqEntity.setLocale(request.locale());
        reqEntity.setLocationHint(request.locationHint());
        reqEntity.setMaxResults(request.maxResults());
        reqEntity.setStatus(RecommendationStatus.RECEIVED);

        reqEntity = requestRepository.save(reqEntity);

        UUID correlationId = UUID.randomUUID();

        processRecommendationAsync(reqEntity.getId(), null, correlationId);

        String basePath = "/api/v1/vet-recommendations/" + reqEntity.getId();
        return new CreateRecommendationResponse(
                reqEntity.getId(),
                reqEntity.getStatus().name(),
                reqEntity.getCreatedAt(),
                Map.of(
                        "self", basePath,
                        "status", basePath + "/status",
                        "result", basePath + "/result"
                )
        );
    }

    @Async
    public void processRecommendationAsync(UUID requestId, String promptVersionOverride, UUID correlationId) {
        long startTime = System.currentTimeMillis();

        VetRecommendationRequest request = requestRepository.findById(requestId).orElse(null);
        if (request == null) {
            log.error("Solicitud de recomendacion no encontrada para procesamiento asincrono: {}", requestId);
            return;
        }

        try {
            // Step 1: INTERPRETING
            updateStatus(request, RecommendationStatus.INTERPRETING, correlationId);
            String effectiveVersion = promptBuilder.getEffectiveVersion(promptVersionOverride);

            // Step 2: Call ClientCaseInterpreterService
            ClientCaseInterpreterService.InterpretationResult interpretationResult =
                    interpreterService.interpret(
                            request.getClientText(),
                            request.getLocale(),
                            request.getLocationHint(),
                            promptVersionOverride
                    );

            auditService.logPromptSent(requestId, correlationId, effectiveVersion,
                    "SYSTEM:\n" + interpretationResult.systemPrompt() + "\n\nUSER:\n" + interpretationResult.userPrompt(),
                    request.getRetryCount() + 1);

            String interpretedJsonStr = objectMapper.writeValueAsString(interpretationResult.interpretedJson());
            auditService.logLlmResponse(requestId, correlationId, interpretedJsonStr,
                    0, request.getRetryCount() + 1);

            request.setLlmProvider(interpreterService.getProviderName());
            request.setLlmModel(interpreterService.getModelName());
            request.setPromptVersion(effectiveVersion);
            request.setInputTokens(interpretationResult.inputTokens());
            request.setOutputTokens(interpretationResult.outputTokens());

            // Step 3: Save interpretedJson, update status
            request.setInterpretedJson(interpretedJsonStr);
            updateStatus(request, RecommendationStatus.INTERPRETATION_COMPLETED, correlationId);

            // Step 4: Validate
            JsonNode interpretedJson = interpretationResult.interpretedJson();
            ExtractionValidatorService.ValidationResult validationResult = validatorService.validate(interpretedJson);

            String validationDetails = objectMapper.writeValueAsString(Map.of(
                    "valid", validationResult.valid(),
                    "errors", validationResult.errors(),
                    "warnings", validationResult.warnings()
            ));
            auditService.logValidationResult(requestId, correlationId, validationDetails);

            // Step 5: Check validation
            if (!validationResult.valid()) {
                request.setFailureReason("INTERPRETATION_VALIDATION_ERROR");
                request.setFailureMessage(String.join("; ", validationResult.errors()));
                request.setProcessingDurationMs((int) (System.currentTimeMillis() - startTime));
                updateStatus(request, RecommendationStatus.INTERPRETATION_FAILED, correlationId);
                requestRepository.save(request);
                return;
            }
            updateStatus(request, RecommendationStatus.INTERPRETATION_VALIDATED, correlationId);

            // Step 6: Normalize interpretation
            ObjectNode normalizedInterpretation = normalizeInterpretation(interpretedJson);

            String normalizedStr = objectMapper.writeValueAsString(normalizedInterpretation);
            request.setNormalizedInterpretation(normalizedStr);

            auditService.logNormalizationResult(requestId, correlationId,
                    objectMapper.writeValueAsString(Map.of("status", "OK")));

            // Extract confidence
            if (normalizedInterpretation.has("confidence") && !normalizedInterpretation.get("confidence").isNull()) {
                double confidence = normalizedInterpretation.get("confidence").asDouble();
                request.setInterpretationConfidence(BigDecimal.valueOf(confidence));
            }

            updateStatus(request, RecommendationStatus.NORMALIZATION_COMPLETED, correlationId);

            // Step 8: Extract normalized symptom codes
            Set<String> symptomCodes = extractSymptomCodes(normalizedInterpretation);

            // Extract species and location
            String species = normalizedInterpretation.has("species") && !normalizedInterpretation.get("species").isNull()
                    ? normalizedInterpretation.get("species").asText()
                    : null;

            String country = null;
            String region = null;
            JsonNode locationNode = normalizedInterpretation.get("location");
            if (locationNode != null && locationNode.isObject()) {
                country = locationNode.has("country") && !locationNode.get("country").isNull()
                        ? locationNode.get("country").asText() : null;
                region = locationNode.has("region") && !locationNode.get("region").isNull()
                        ? locationNode.get("region").asText() : null;
            }

            // Step 9: Call CaseMatchingService
            updateStatus(request, RecommendationStatus.MATCHING, correlationId);

            CaseMatchingService.MatchingOutput matchingOutput =
                    matchingService.findSimilarCases(species, symptomCodes, country, region);

            request.setTotalCasesSearched(matchingOutput.totalCasesSearched());

            // Step 11: Persist individual matches
            List<RecommendationMatch> persistedMatches = new ArrayList<>();
            for (CaseMatchingService.MatchResult match : matchingOutput.matches()) {
                RecommendationMatch matchEntity = new RecommendationMatch();
                matchEntity.setRequestId(requestId);
                matchEntity.setStructuredReviewId(match.review().getId());
                matchEntity.setVetName(match.review().getVetName());
                matchEntity.setSimilarityScore(BigDecimal.valueOf(match.similarityScore()));
                matchEntity.setSpeciesMatch(match.speciesMatch());
                matchEntity.setSymptomOverlapCodes(objectMapper.writeValueAsString(match.overlappingSymptoms()));
                matchEntity.setSymptomJaccard(BigDecimal.valueOf(match.symptomJaccard()));
                matchEntity.setLocationScore(BigDecimal.valueOf(match.locationScore()));
                matchEntity.setOutcomeStatus(match.review().getOutcomeStatus());
                persistedMatches.add(matchRepository.save(matchEntity));
            }

            request.setTotalMatchesFound(persistedMatches.size());

            // Step 12: Score and rank
            List<VetScoringService.ScoredVet> scoredVets =
                    scoringService.scoreAndRank(matchingOutput.matches(), request.getMaxResults());

            updateStatus(request, RecommendationStatus.SCORING_COMPLETED, correlationId);

            // Step 13: Persist VetRecommendation entities
            int rank = 1;
            for (VetScoringService.ScoredVet scoredVet : scoredVets) {
                VetRecommendation rec = new VetRecommendation();
                rec.setRequestId(requestId);
                rec.setRankPosition(rank++);
                rec.setVetName(scoredVet.vetName());
                rec.setVetClinic(scoredVet.vetClinic());
                rec.setLocationCountry(scoredVet.locationCountry());
                rec.setLocationRegion(scoredVet.locationRegion());
                rec.setScore(BigDecimal.valueOf(scoredVet.score()));
                rec.setSimilarCasesCount(scoredVet.similarCasesCount());
                rec.setPositiveOutcomeRate(BigDecimal.valueOf(scoredVet.positiveOutcomeRate()));
                rec.setAvgSimilarity(BigDecimal.valueOf(scoredVet.avgSimilarity()));
                rec.setLocationMatch(scoredVet.locationMatch());

                // Build explanation JSON from ScoredVet fields
                Map<String, Object> explanationMap = new LinkedHashMap<>();
                explanationMap.put("summary", scoredVet.summary());
                explanationMap.put("matchedSymptoms", scoredVet.matchedSymptoms());
                explanationMap.put("outcomeBreakdown", scoredVet.outcomeBreakdown());
                explanationMap.put("locationMatch", scoredVet.locationMatch());
                rec.setExplanationJson(objectMapper.writeValueAsString(explanationMap));

                recommendationRepository.save(rec);
            }

            // Step 14: COMPLETED
            request.setProcessingDurationMs((int) (System.currentTimeMillis() - startTime));
            request.setFailureReason(null);
            request.setFailureMessage(null);
            updateStatus(request, RecommendationStatus.COMPLETED, correlationId);
            requestRepository.save(request);

            log.info("Recomendacion {} procesada exitosamente en {}ms", requestId, request.getProcessingDurationMs());

        } catch (JsonProcessingException e) {
            handleProcessingError(request, correlationId, "JSON_PARSE_ERROR", e.getMessage(), startTime);
        } catch (Exception e) {
            handleProcessingError(request, correlationId, "UNEXPECTED_ERROR", e.getMessage(), startTime);
            log.error("Error inesperado procesando recomendacion {}", requestId, e);
        }
    }

    public RecommendationResultResponse getResult(UUID requestId) {
        VetRecommendationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RecommendationNotFoundException(requestId));

        if (request.getStatus() != RecommendationStatus.COMPLETED) {
            return null;
        }

        try {
            // Build interpretation from normalizedInterpretation
            RecommendationResultResponse.Interpretation interpretation = buildInterpretation(request);

            // Build recommendations list
            List<VetRecommendation> vetRecommendations =
                    recommendationRepository.findByRequestIdOrderByRankPositionAsc(requestId);

            List<RecommendationResultResponse.VetRecommendationDto> recommendationDtos = new ArrayList<>();
            for (VetRecommendation rec : vetRecommendations) {
                RecommendationResultResponse.VetExplanation explanation = buildExplanation(rec);

                recommendationDtos.add(new RecommendationResultResponse.VetRecommendationDto(
                        rec.getRankPosition(),
                        rec.getVetName(),
                        rec.getVetClinic(),
                        new RecommendationResultResponse.VetLocation(
                                rec.getLocationCountry(),
                                rec.getLocationRegion()
                        ),
                        rec.getScore() != null ? rec.getScore().doubleValue() : null,
                        rec.getSimilarCasesCount(),
                        rec.getPositiveOutcomeRate() != null ? rec.getPositiveOutcomeRate().doubleValue() : null,
                        rec.getAvgSimilarity() != null ? rec.getAvgSimilarity().doubleValue() : null,
                        explanation
                ));
            }

            // Build metadata
            RecommendationResultResponse.RecommendationMetadata metadata =
                    new RecommendationResultResponse.RecommendationMetadata(
                            request.getTotalCasesSearched(),
                            request.getTotalMatchesFound(),
                            request.getProcessingDurationMs() != null ? request.getProcessingDurationMs().longValue() : null,
                            request.getLlmProvider(),
                            request.getLlmModel(),
                            request.getPromptVersion()
                    );

            return new RecommendationResultResponse(
                    request.getId(),
                    request.getStatus().name(),
                    interpretation,
                    recommendationDtos,
                    metadata
            );

        } catch (JsonProcessingException e) {
            log.error("Error mapeando resultado de recomendacion para request {}", requestId, e);
            throw new RuntimeException("Error mapeando resultado de recomendacion", e);
        }
    }

    public RecommendationStatusResponse getStatus(UUID requestId) {
        VetRecommendationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RecommendationNotFoundException(requestId));

        String currentStep = determineCurrentStep(request.getStatus());
        List<String> stepsCompleted = determineStepsCompleted(request.getStatus());
        boolean retriable = request.getStatus() == RecommendationStatus.FAILED
                || request.getStatus() == RecommendationStatus.INTERPRETATION_FAILED
                || request.getStatus() == RecommendationStatus.NORMALIZATION_FAILED
                || request.getStatus() == RecommendationStatus.MATCHING_FAILED;

        return new RecommendationStatusResponse(
                request.getId(),
                request.getStatus().name(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                currentStep,
                stepsCompleted,
                request.getRetryCount(),
                request.getFailureReason(),
                request.getFailureMessage(),
                retriable
        );
    }

    @Transactional
    public RecalculateRecommendationResponse recalculate(UUID requestId, RecalculateRecommendationRequest recalcRequest) {
        VetRecommendationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RecommendationNotFoundException(requestId));

        if (request.getStatus() == RecommendationStatus.INTERPRETING
                || request.getStatus() == RecommendationStatus.MATCHING
                || request.getStatus() == RecommendationStatus.REPROCESSING) {
            throw new RecommendationAlreadyProcessingException(requestId);
        }

        if (request.getStatus() == RecommendationStatus.COMPLETED && !recalcRequest.forceRecalculate()) {
            throw new IllegalStateException("Recomendacion completada. Use forceRecalculate=true para forzar.");
        }

        RecommendationStatus previousStatus = request.getStatus();
        UUID correlationId = UUID.randomUUID();

        request.setRecalculateCount(request.getRecalculateCount() + 1);
        request.setRetryCount(0);
        request.setFailureReason(null);
        request.setFailureMessage(null);

        // Clean previous results
        recommendationRepository.deleteByRequestId(requestId);
        matchRepository.deleteByRequestId(requestId);

        updateStatus(request, RecommendationStatus.REPROCESSING, correlationId);

        processRecommendationAsync(requestId, recalcRequest.promptVersionOverride(), correlationId);

        return new RecalculateRecommendationResponse(
                requestId,
                previousStatus.name(),
                RecommendationStatus.REPROCESSING.name(),
                recalcRequest.reason(),
                request.getRecalculateCount(),
                request.getUpdatedAt()
        );
    }

    // --- Private helpers ---

    private ObjectNode normalizeInterpretation(JsonNode interpretedJson) {
        ObjectNode normalized = interpretedJson.deepCopy();

        // Normalize species
        if (normalized.has("species") && !normalized.get("species").isNull()) {
            String species = normalized.get("species").asText();
            String normalizedSpecies = normalizationService.resolveCode("ANIMAL_TYPE", species);
            normalized.put("species", normalizedSpecies);
        }

        // Normalize symptoms
        JsonNode symptoms = normalized.get("symptoms");
        if (symptoms != null && symptoms.isArray()) {
            ArrayNode normalizedSymptoms = objectMapper.createArrayNode();
            for (JsonNode symptom : symptoms) {
                ObjectNode s = symptom.deepCopy();
                if (s.has("suggested_code") && !s.get("suggested_code").isNull()) {
                    String code = s.get("suggested_code").asText();
                    s.put("normalized_code", normalizationService.resolveCode("SYMPTOM", code));
                }
                if (s.has("body_area") && !s.get("body_area").isNull()) {
                    String bodyArea = s.get("body_area").asText();
                    s.put("body_area", normalizationService.resolveCode("BODY_AREA", bodyArea));
                }
                normalizedSymptoms.add(s);
            }
            normalized.set("symptoms", normalizedSymptoms);
        }

        return normalized;
    }

    private Set<String> extractSymptomCodes(JsonNode normalizedInterpretation) {
        Set<String> codes = new LinkedHashSet<>();
        JsonNode symptoms = normalizedInterpretation.get("symptoms");
        if (symptoms != null && symptoms.isArray()) {
            for (JsonNode symptom : symptoms) {
                if (symptom.has("normalized_code") && !symptom.get("normalized_code").isNull()) {
                    codes.add(symptom.get("normalized_code").asText());
                } else if (symptom.has("suggested_code") && !symptom.get("suggested_code").isNull()) {
                    codes.add(symptom.get("suggested_code").asText());
                }
            }
        }
        return codes;
    }

    private RecommendationResultResponse.Interpretation buildInterpretation(VetRecommendationRequest request)
            throws JsonProcessingException {
        JsonNode normalizedJson = objectMapper.readTree(request.getNormalizedInterpretation());

        String species = normalizedJson.has("species") && !normalizedJson.get("species").isNull()
                ? normalizedJson.get("species").asText() : null;

        String urgency = normalizedJson.has("urgency") && !normalizedJson.get("urgency").isNull()
                ? normalizedJson.get("urgency").asText() : null;

        String conditionHint = normalizedJson.has("condition_hint") && !normalizedJson.get("condition_hint").isNull()
                ? normalizedJson.get("condition_hint").asText() : null;

        Double confidence = normalizedJson.has("confidence") && !normalizedJson.get("confidence").isNull()
                ? normalizedJson.get("confidence").asDouble() : null;

        // Build symptoms
        List<RecommendationResultResponse.InterpretedSymptom> symptoms = new ArrayList<>();
        JsonNode symptomsNode = normalizedJson.get("symptoms");
        if (symptomsNode != null && symptomsNode.isArray()) {
            for (JsonNode s : symptomsNode) {
                symptoms.add(new RecommendationResultResponse.InterpretedSymptom(
                        s.has("normalized_code") ? s.get("normalized_code").asText()
                                : (s.has("suggested_code") ? s.get("suggested_code").asText() : null),
                        s.has("description") ? s.get("description").asText() : null,
                        s.has("body_area") && !s.get("body_area").isNull() ? s.get("body_area").asText() : null,
                        s.has("duration") && !s.get("duration").isNull() ? s.get("duration").asText() : null
                ));
            }
        }

        // Build location
        RecommendationResultResponse.InterpretedLocation location = null;
        JsonNode locationNode = normalizedJson.get("location");
        if (locationNode != null && locationNode.isObject()) {
            location = new RecommendationResultResponse.InterpretedLocation(
                    locationNode.has("country") && !locationNode.get("country").isNull()
                            ? locationNode.get("country").asText() : null,
                    locationNode.has("region") && !locationNode.get("region").isNull()
                            ? locationNode.get("region").asText() : null,
                    locationNode.has("city") && !locationNode.get("city").isNull()
                            ? locationNode.get("city").asText() : null
            );
        }

        return new RecommendationResultResponse.Interpretation(
                species, symptoms, urgency, conditionHint, location, confidence
        );
    }

    private RecommendationResultResponse.VetExplanation buildExplanation(VetRecommendation rec)
            throws JsonProcessingException {
        JsonNode explanationNode = objectMapper.readTree(rec.getExplanationJson());

        String summary = explanationNode.has("summary") ? explanationNode.get("summary").asText() : null;

        List<String> matchedSymptoms = new ArrayList<>();
        if (explanationNode.has("matchedSymptoms") && explanationNode.get("matchedSymptoms").isArray()) {
            for (JsonNode s : explanationNode.get("matchedSymptoms")) {
                matchedSymptoms.add(s.asText());
            }
        }

        Map<String, Integer> outcomeBreakdown = new LinkedHashMap<>();
        if (explanationNode.has("outcomeBreakdown") && explanationNode.get("outcomeBreakdown").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = explanationNode.get("outcomeBreakdown").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                outcomeBreakdown.put(entry.getKey(), entry.getValue().asInt());
            }
        }

        Boolean locationMatch = explanationNode.has("locationMatch") && !explanationNode.get("locationMatch").isNull()
                ? explanationNode.get("locationMatch").asBoolean() : null;

        return new RecommendationResultResponse.VetExplanation(
                summary, matchedSymptoms, outcomeBreakdown, locationMatch
        );
    }

    private void handleProcessingError(VetRecommendationRequest request, UUID correlationId,
                                       String errorCode, String message, long startTime) {
        request.setFailureReason(errorCode);
        request.setFailureMessage(message);
        request.setProcessingDurationMs((int) (System.currentTimeMillis() - startTime));
        request.setRetryCount(request.getRetryCount() + 1);
        updateStatus(request, RecommendationStatus.FAILED, correlationId);
        requestRepository.save(request);
        auditService.logError(request.getId(), correlationId, errorCode, message, request.getRetryCount());
    }

    private void updateStatus(VetRecommendationRequest request, RecommendationStatus newStatus, UUID correlationId) {
        RecommendationStatus oldStatus = request.getStatus();
        request.setStatus(newStatus);
        requestRepository.save(request);
        log.debug("Recomendacion {} cambio de estado: {} -> {}", request.getId(), oldStatus, newStatus);
    }

    private String determineCurrentStep(RecommendationStatus status) {
        return switch (status) {
            case RECEIVED -> "RECEIVED";
            case INTERPRETING, REPROCESSING -> "INTERPRETATION";
            case INTERPRETATION_COMPLETED -> "VALIDATION";
            case INTERPRETATION_VALIDATED -> "NORMALIZATION";
            case INTERPRETATION_FAILED -> "FAILED";
            case NORMALIZATION_COMPLETED -> "MATCHING";
            case NORMALIZATION_FAILED -> "FAILED";
            case MATCHING -> "MATCHING";
            case MATCHING_FAILED -> "FAILED";
            case SCORING_COMPLETED -> "SCORING";
            case COMPLETED -> "COMPLETED";
            case FAILED -> "FAILED";
        };
    }

    private List<String> determineStepsCompleted(RecommendationStatus status) {
        List<String> steps = new ArrayList<>();
        steps.add("RECEIVED");

        if (status.ordinal() > RecommendationStatus.RECEIVED.ordinal()
                && status != RecommendationStatus.FAILED) {
            steps.add("INTERPRETING");
        }
        if (status.ordinal() >= RecommendationStatus.INTERPRETATION_COMPLETED.ordinal()
                && status != RecommendationStatus.FAILED
                && status != RecommendationStatus.INTERPRETATION_FAILED) {
            steps.add("INTERPRETATION_COMPLETED");
        }
        if (status.ordinal() >= RecommendationStatus.INTERPRETATION_VALIDATED.ordinal()
                && status != RecommendationStatus.FAILED
                && status != RecommendationStatus.INTERPRETATION_FAILED) {
            steps.add("INTERPRETATION_VALIDATED");
        }
        if (status.ordinal() >= RecommendationStatus.NORMALIZATION_COMPLETED.ordinal()
                && status != RecommendationStatus.FAILED
                && status != RecommendationStatus.INTERPRETATION_FAILED
                && status != RecommendationStatus.NORMALIZATION_FAILED) {
            steps.add("NORMALIZATION_COMPLETED");
        }
        if (status.ordinal() >= RecommendationStatus.MATCHING.ordinal()
                && status != RecommendationStatus.FAILED
                && status != RecommendationStatus.INTERPRETATION_FAILED
                && status != RecommendationStatus.NORMALIZATION_FAILED
                && status != RecommendationStatus.MATCHING_FAILED) {
            steps.add("MATCHING");
        }
        if (status.ordinal() >= RecommendationStatus.SCORING_COMPLETED.ordinal()
                && status != RecommendationStatus.FAILED) {
            steps.add("SCORING_COMPLETED");
        }
        if (status == RecommendationStatus.COMPLETED) {
            steps.add("COMPLETED");
        }

        return steps;
    }
}
