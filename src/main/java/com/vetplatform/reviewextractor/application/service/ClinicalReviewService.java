package com.vetplatform.reviewextractor.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.domain.entity.ClinicalReview;
import com.vetplatform.reviewextractor.domain.entity.StructuredClinicalReview;
import com.vetplatform.reviewextractor.domain.enums.ReviewStatus;
import com.vetplatform.reviewextractor.domain.repository.ClinicalReviewRepository;
import com.vetplatform.reviewextractor.domain.repository.StructuredClinicalReviewRepository;
import com.vetplatform.reviewextractor.dto.request.CreateClinicalReviewRequest;
import com.vetplatform.reviewextractor.dto.request.ReprocessReviewRequest;
import com.vetplatform.reviewextractor.dto.response.*;
import com.vetplatform.reviewextractor.infrastructure.exception.LlmInvocationException;
import com.vetplatform.reviewextractor.infrastructure.exception.ReviewAlreadyProcessingException;
import com.vetplatform.reviewextractor.infrastructure.exception.ReviewNotFoundException;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmExtractionClient;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmExtractionClient.LlmResponse;
import com.vetplatform.reviewextractor.infrastructure.llm.LlmResponseParser;
import com.vetplatform.reviewextractor.infrastructure.llm.PromptBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ClinicalReviewService {

    private static final Logger log = LoggerFactory.getLogger(ClinicalReviewService.class);

    private final ClinicalReviewRepository reviewRepository;
    private final StructuredClinicalReviewRepository structuredRepository;
    private final PromptBuilderService promptBuilder;
    private final LlmExtractionClient llmClient;
    private final LlmResponseParser responseParser;
    private final ExtractionValidatorService validator;
    private final ClinicalNormalizationService normalizer;
    private final ProcessingAuditService auditService;
    private final ObjectMapper objectMapper;

    public ClinicalReviewService(
            ClinicalReviewRepository reviewRepository,
            StructuredClinicalReviewRepository structuredRepository,
            PromptBuilderService promptBuilder,
            LlmExtractionClient llmClient,
            LlmResponseParser responseParser,
            ExtractionValidatorService validator,
            ClinicalNormalizationService normalizer,
            ProcessingAuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.reviewRepository = reviewRepository;
        this.structuredRepository = structuredRepository;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.responseParser = responseParser;
        this.validator = validator;
        this.normalizer = normalizer;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateClinicalReviewResponse createReview(CreateClinicalReviewRequest request) {
        ClinicalReview review = new ClinicalReview();
        review.setRawText(request.reviewText());
        review.setPetOwnerId(request.petOwnerId());
        review.setLocale(request.locale());
        review.setStatus(ReviewStatus.RECEIVED);

        review = reviewRepository.save(review);

        UUID correlationId = UUID.randomUUID();
        auditService.logStatusChange(review.getId(), correlationId, null, ReviewStatus.RECEIVED);

        processReviewAsync(review.getId(), null, correlationId);

        String basePath = "/api/v1/clinical-reviews/" + review.getId();
        return new CreateClinicalReviewResponse(
                review.getId(),
                review.getStatus().name(),
                review.getCreatedAt(),
                Map.of(
                        "self", basePath,
                        "status", basePath + "/status",
                        "result", basePath + "/result"
                )
        );
    }

    @Async
    public void processReviewAsync(UUID reviewId, String promptVersionOverride, UUID correlationId) {
        long startTime = System.currentTimeMillis();

        ClinicalReview review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            log.error("Review no encontrado para procesamiento asincrono: {}", reviewId);
            return;
        }

        try {
            updateStatus(review, ReviewStatus.PROCESSING, correlationId);
            String effectiveVersion = promptBuilder.getEffectiveVersion(promptVersionOverride);

            // Build prompt
            String systemPrompt = (promptVersionOverride != null)
                    ? promptBuilder.buildSystemPrompt(promptVersionOverride)
                    : promptBuilder.buildSystemPrompt();
            String userPrompt = (promptVersionOverride != null)
                    ? promptBuilder.buildUserPrompt(review.getRawText(), review.getLocale(), promptVersionOverride)
                    : promptBuilder.buildUserPrompt(review.getRawText(), review.getLocale());

            String fullPrompt = "SYSTEM:\n" + systemPrompt + "\n\nUSER:\n" + userPrompt;
            auditService.logPromptSent(reviewId, correlationId, effectiveVersion, fullPrompt, review.getRetryCount() + 1);

            // Invoke LLM
            long llmStart = System.currentTimeMillis();
            LlmResponse llmResponse = llmClient.extract(systemPrompt, userPrompt);
            int llmDurationMs = (int) (System.currentTimeMillis() - llmStart);

            auditService.logLlmResponse(reviewId, correlationId, llmResponse.content(), llmDurationMs, review.getRetryCount() + 1);

            review.setLlmProvider(llmClient.getProviderName());
            review.setLlmModel(llmClient.getModelName());
            review.setPromptVersion(effectiveVersion);
            review.setInputTokens(llmResponse.inputTokens());
            review.setOutputTokens(llmResponse.outputTokens());

            // Parse response
            JsonNode extractedJson = parseWithRetry(llmResponse.content(), reviewId, correlationId);
            review.setExtractedJson(objectMapper.writeValueAsString(extractedJson));

            updateStatus(review, ReviewStatus.EXTRACTION_COMPLETED, correlationId);

            // Validate
            ExtractionValidatorService.ValidationResult validationResult = validator.validate(extractedJson);
            String validationDetails = objectMapper.writeValueAsString(Map.of(
                    "valid", validationResult.valid(),
                    "errors", validationResult.errors(),
                    "warnings", validationResult.warnings()
            ));
            auditService.logValidationResult(reviewId, correlationId, validationDetails);

            if (!validationResult.valid()) {
                review.setFailureReason("VALIDATION_ERROR");
                review.setFailureMessage(String.join("; ", validationResult.errors()));
                updateStatus(review, ReviewStatus.VALIDATION_FAILED, correlationId);
                review.setProcessingDurationMs((int) (System.currentTimeMillis() - startTime));
                reviewRepository.save(review);
                return;
            }

            updateStatus(review, ReviewStatus.VALIDATION_PASSED, correlationId);

            // Normalize
            JsonNode normalizedJson = normalizer.normalize(extractedJson);
            String normalizedStr = objectMapper.writeValueAsString(normalizedJson);
            review.setNormalizedJson(normalizedStr);

            auditService.logNormalizationResult(reviewId, correlationId,
                    objectMapper.writeValueAsString(Map.of("status", "OK")));

            // Extract confidence
            if (normalizedJson.has("confidence") && !normalizedJson.get("confidence").isNull()) {
                double confidence = normalizedJson.get("confidence").asDouble();
                review.setOverallConfidence(BigDecimal.valueOf(confidence));
            }

            // Persist structured result
            persistStructuredReview(reviewId, normalizedJson);

            review.setProcessingDurationMs((int) (System.currentTimeMillis() - startTime));
            review.setFailureReason(null);
            review.setFailureMessage(null);
            updateStatus(review, ReviewStatus.COMPLETED, correlationId);
            reviewRepository.save(review);

            log.info("Review {} procesado exitosamente en {}ms", reviewId, review.getProcessingDurationMs());

        } catch (LlmInvocationException e) {
            handleProcessingError(review, correlationId, "LLM_INVOCATION_ERROR", e.getMessage(), startTime);
        } catch (JsonProcessingException e) {
            handleProcessingError(review, correlationId, "JSON_PARSE_ERROR", e.getMessage(), startTime);
        } catch (Exception e) {
            handleProcessingError(review, correlationId, "UNEXPECTED_ERROR", e.getMessage(), startTime);
            log.error("Error inesperado procesando review {}", reviewId, e);
        }
    }

    private JsonNode parseWithRetry(String content, UUID reviewId, UUID correlationId) throws JsonProcessingException {
        if (responseParser.isValidJson(content)) {
            return responseParser.parse(content);
        }

        auditService.logError(reviewId, correlationId, "JSON_MALFORMED",
                "Primera respuesta con JSON invalido, reintentando", 1);

        // Retry with a correction prompt
        String correctionPrompt = "Tu respuesta anterior no fue un JSON valido. " +
                "Por favor responde SOLO con el JSON estructurado, sin texto adicional.";
        LlmResponse retryResponse = llmClient.extract(
                promptBuilder.buildSystemPrompt(),
                correctionPrompt + "\n\nTexto original de la respuesta:\n" + content
        );

        if (!responseParser.isValidJson(retryResponse.content())) {
            throw new JsonProcessingException("JSON invalido tras reintento") {};
        }

        return responseParser.parse(retryResponse.content());
    }

    @Transactional
    void persistStructuredReview(UUID reviewId, JsonNode normalizedJson) {
        StructuredClinicalReview structured = new StructuredClinicalReview();
        structured.setReviewId(reviewId);

        // Set species
        if (normalizedJson.has("species") && !normalizedJson.get("species").isNull()) {
            structured.setSpecies(normalizedJson.get("species").asText());
        }

        // Set breed
        if (normalizedJson.has("breed") && !normalizedJson.get("breed").isNull()) {
            structured.setBreed(normalizedJson.get("breed").asText());
        }

        // Set pet_name
        if (normalizedJson.has("pet_name") && !normalizedJson.get("pet_name").isNull()) {
            structured.setPetName(normalizedJson.get("pet_name").asText());
        }

        // Set arrays as JSON strings
        if (normalizedJson.has("symptoms")) {
            structured.setSymptomsJson(normalizedJson.get("symptoms").toString());
        }
        if (normalizedJson.has("procedures")) {
            structured.setProceduresJson(normalizedJson.get("procedures").toString());
        }
        if (normalizedJson.has("medications")) {
            structured.setMedicationsJson(normalizedJson.get("medications").toString());
        }

        // Veterinarian
        JsonNode vet = normalizedJson.get("veterinarian");
        if (vet != null && vet.isObject()) {
            if (vet.has("name") && !vet.get("name").isNull()) {
                structured.setVetName(vet.get("name").asText());
            }
            if (vet.has("clinic") && !vet.get("clinic").isNull()) {
                structured.setVetClinic(vet.get("clinic").asText());
            }
        }

        // Location
        JsonNode location = normalizedJson.get("location");
        if (location != null && location.isObject()) {
            if (location.has("raw") && !location.get("raw").isNull()) {
                structured.setLocationRaw(location.get("raw").asText());
            }
            if (location.has("city") && !location.get("city").isNull()) {
                structured.setLocationNormalized(location.get("city").asText());
            }
            if (location.has("region") && !location.get("region").isNull()) {
                structured.setLocationRegion(location.get("region").asText());
            }
            if (location.has("country") && !location.get("country").isNull()) {
                structured.setLocationCountry(location.get("country").asText());
            }
        }

        // Outcome
        JsonNode outcome = normalizedJson.get("outcome");
        if (outcome != null && outcome.isObject()) {
            if (outcome.has("status") && !outcome.get("status").isNull()) {
                structured.setOutcomeStatus(outcome.get("status").asText());
            }
            if (outcome.has("description") && !outcome.get("description").isNull()) {
                structured.setOutcomeDescription(outcome.get("description").asText());
            }
        }

        // Confidence
        if (normalizedJson.has("confidence") && !normalizedJson.get("confidence").isNull()) {
            structured.setOverallConfidence(BigDecimal.valueOf(normalizedJson.get("confidence").asDouble()));
        }

        // Extraction notes
        if (normalizedJson.has("extraction_notes") && !normalizedJson.get("extraction_notes").isNull()) {
            String notes = normalizedJson.get("extraction_notes").asText();
            structured.setExtractionNotes(notes.length() > 500 ? notes.substring(0, 500) : notes);
        }

        // Determine version
        structuredRepository.findTopByReviewIdOrderByVersionDesc(reviewId)
                .ifPresent(existing -> structured.setVersion(existing.getVersion() + 1));

        structuredRepository.save(structured);
    }

    private void handleProcessingError(ClinicalReview review, UUID correlationId, String errorCode, String message, long startTime) {
        review.setFailureReason(errorCode);
        review.setFailureMessage(message);
        review.setProcessingDurationMs((int) (System.currentTimeMillis() - startTime));
        review.setRetryCount(review.getRetryCount() + 1);
        updateStatus(review, ReviewStatus.FAILED, correlationId);
        reviewRepository.save(review);
        auditService.logError(review.getId(), correlationId, errorCode, message, review.getRetryCount());
    }

    private void updateStatus(ClinicalReview review, ReviewStatus newStatus, UUID correlationId) {
        ReviewStatus oldStatus = review.getStatus();
        review.setStatus(newStatus);
        reviewRepository.save(review);
        auditService.logStatusChange(review.getId(), correlationId, oldStatus, newStatus);
    }

    // --- Query methods ---

    public StructuredClinicalReviewResponse getResult(UUID reviewId) {
        ClinicalReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (review.getStatus() != ReviewStatus.COMPLETED) {
            return null; // Controller will handle 409
        }

        StructuredClinicalReview structured = structuredRepository
                .findTopByReviewIdOrderByVersionDesc(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        return mapToStructuredResponse(review, structured);
    }

    public ReviewProcessingStatusResponse getStatus(UUID reviewId) {
        ClinicalReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        String currentStep = determineCurrentStep(review.getStatus());
        List<String> stepsCompleted = determineStepsCompleted(review.getStatus());
        boolean retriable = review.getStatus() == ReviewStatus.FAILED
                || review.getStatus() == ReviewStatus.VALIDATION_FAILED;

        return new ReviewProcessingStatusResponse(
                review.getId(),
                review.getStatus().name(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                currentStep,
                stepsCompleted,
                review.getRetryCount(),
                review.getFailureReason(),
                review.getFailureMessage(),
                retriable
        );
    }

    @Transactional
    public ReprocessReviewResponse reprocess(UUID reviewId, ReprocessReviewRequest request) {
        ClinicalReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (review.getStatus() == ReviewStatus.PROCESSING || review.getStatus() == ReviewStatus.REPROCESSING) {
            throw new ReviewAlreadyProcessingException(reviewId);
        }

        if (review.getStatus() == ReviewStatus.COMPLETED && !request.forceReprocess()) {
            throw new IllegalStateException("Review completado. Use forceReprocess=true para forzar.");
        }

        ReviewStatus previousStatus = review.getStatus();
        UUID correlationId = UUID.randomUUID();

        review.setReprocessCount(review.getReprocessCount() + 1);
        review.setRetryCount(0);
        review.setFailureReason(null);
        review.setFailureMessage(null);
        updateStatus(review, ReviewStatus.REPROCESSING, correlationId);

        processReviewAsync(reviewId, request.promptVersionOverride(), correlationId);

        return new ReprocessReviewResponse(
                reviewId,
                previousStatus.name(),
                ReviewStatus.REPROCESSING.name(),
                request.reason(),
                review.getReprocessCount(),
                review.getUpdatedAt()
        );
    }

    private StructuredClinicalReviewResponse mapToStructuredResponse(ClinicalReview review, StructuredClinicalReview structured) {
        try {
            // Parse symptoms, procedures, medications from JSON strings
            JsonNode symptomsNode = objectMapper.readTree(structured.getSymptomsJson());
            JsonNode proceduresNode = objectMapper.readTree(structured.getProceduresJson());
            JsonNode medicationsNode = objectMapper.readTree(structured.getMedicationsJson());

            List<StructuredClinicalReviewResponse.Symptom> symptoms = new ArrayList<>();
            for (JsonNode s : symptomsNode) {
                symptoms.add(new StructuredClinicalReviewResponse.Symptom(
                        s.has("description") ? s.get("description").asText() : null,
                        s.has("normalized_code") ? s.get("normalized_code").asText() :
                                (s.has("suggested_code") ? s.get("suggested_code").asText() : null),
                        s.has("body_area") && !s.get("body_area").isNull() ? s.get("body_area").asText() : null
                ));
            }

            List<StructuredClinicalReviewResponse.Procedure> procedures = new ArrayList<>();
            for (JsonNode p : proceduresNode) {
                procedures.add(new StructuredClinicalReviewResponse.Procedure(
                        p.has("description") ? p.get("description").asText() : null,
                        p.has("normalized_code") ? p.get("normalized_code").asText() :
                                (p.has("suggested_code") ? p.get("suggested_code").asText() : null),
                        p.has("type") && !p.get("type").isNull() ? p.get("type").asText() : null
                ));
            }

            List<StructuredClinicalReviewResponse.Medication> medications = new ArrayList<>();
            for (JsonNode m : medicationsNode) {
                medications.add(new StructuredClinicalReviewResponse.Medication(
                        m.has("name") ? m.get("name").asText() : null,
                        m.has("dosage") && !m.get("dosage").isNull() ? m.get("dosage").asText() : null,
                        m.has("frequency") && !m.get("frequency").isNull() ? m.get("frequency").asText() : null
                ));
            }

            var extractedData = new StructuredClinicalReviewResponse.ExtractedData(
                    structured.getSpecies(),
                    structured.getBreed(),
                    structured.getPetName(),
                    symptoms,
                    procedures,
                    medications,
                    new StructuredClinicalReviewResponse.Veterinarian(
                            structured.getVetName(),
                            structured.getVetClinic()
                    ),
                    new StructuredClinicalReviewResponse.Location(
                            structured.getLocationRaw(),
                            structured.getLocationNormalized(),
                            structured.getLocationRegion(),
                            structured.getLocationCountry()
                    ),
                    new StructuredClinicalReviewResponse.Outcome(
                            structured.getOutcomeStatus(),
                            structured.getOutcomeDescription()
                    ),
                    structured.getOverallConfidence() != null ? structured.getOverallConfidence().doubleValue() : null,
                    structured.getCreatedAt()
            );

            var metadata = new StructuredClinicalReviewResponse.ProcessingMetadata(
                    review.getLlmProvider(),
                    review.getLlmModel(),
                    review.getPromptVersion(),
                    review.getProcessingDurationMs() != null ? review.getProcessingDurationMs().longValue() : null,
                    new StructuredClinicalReviewResponse.TokenUsage(
                            review.getInputTokens(),
                            review.getOutputTokens()
                    )
            );

            return new StructuredClinicalReviewResponse(
                    review.getId(),
                    review.getStatus().name(),
                    extractedData,
                    metadata
            );

        } catch (JsonProcessingException e) {
            log.error("Error mapeando resultado estructurado para review {}", review.getId(), e);
            throw new RuntimeException("Error mapeando resultado estructurado", e);
        }
    }

    private String determineCurrentStep(ReviewStatus status) {
        return switch (status) {
            case RECEIVED -> "RECEIVED";
            case PROCESSING, REPROCESSING -> "LLM_INVOCATION";
            case EXTRACTION_COMPLETED -> "VALIDATION";
            case VALIDATION_PASSED -> "NORMALIZATION";
            case COMPLETED -> "COMPLETED";
            case FAILED, VALIDATION_FAILED, NORMALIZATION_FAILED -> "FAILED";
        };
    }

    private List<String> determineStepsCompleted(ReviewStatus status) {
        List<String> steps = new ArrayList<>();
        steps.add("RECEIVED");

        if (status.ordinal() > ReviewStatus.RECEIVED.ordinal() && status != ReviewStatus.FAILED) {
            steps.add("PROCESSING");
        }
        if (status.ordinal() >= ReviewStatus.EXTRACTION_COMPLETED.ordinal() && status != ReviewStatus.FAILED) {
            steps.add("EXTRACTION_COMPLETED");
        }
        if (status.ordinal() >= ReviewStatus.VALIDATION_PASSED.ordinal()
                && status != ReviewStatus.VALIDATION_FAILED && status != ReviewStatus.FAILED) {
            steps.add("VALIDATION_PASSED");
        }
        if (status == ReviewStatus.COMPLETED) {
            steps.add("NORMALIZATION_COMPLETED");
            steps.add("COMPLETED");
        }

        return steps;
    }
}
