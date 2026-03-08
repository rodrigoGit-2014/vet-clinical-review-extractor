package com.vetplatform.reviewextractor.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.domain.entity.StructuredClinicalReview;
import com.vetplatform.reviewextractor.domain.repository.StructuredClinicalReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CaseMatchingService {

    private static final Logger log = LoggerFactory.getLogger(CaseMatchingService.class);

    private final StructuredClinicalReviewRepository structuredRepository;
    private final ObjectMapper objectMapper;
    private final double minSimilarityThreshold;
    private final int maxCandidates;

    public CaseMatchingService(
            StructuredClinicalReviewRepository structuredRepository,
            ObjectMapper objectMapper,
            @Value("${matching.min-similarity-threshold:0.3}") double minSimilarityThreshold,
            @Value("${matching.max-candidates:500}") int maxCandidates
    ) {
        this.structuredRepository = structuredRepository;
        this.objectMapper = objectMapper;
        this.minSimilarityThreshold = minSimilarityThreshold;
        this.maxCandidates = maxCandidates;
    }

    public record MatchResult(
            StructuredClinicalReview review,
            double similarityScore,
            boolean speciesMatch,
            Set<String> overlappingSymptoms,
            double symptomJaccard,
            double locationScore
    ) {}

    public record MatchingOutput(
            List<MatchResult> matches,
            int totalCasesSearched
    ) {}

    public MatchingOutput findSimilarCases(
            String species,
            Set<String> normalizedSymptomCodes,
            String locationCountry,
            String locationRegion
    ) {
        if (normalizedSymptomCodes.isEmpty()) {
            log.warn("No hay sintomas normalizados para matching");
            return new MatchingOutput(List.of(), 0);
        }

        String[] symptomCodesArray = normalizedSymptomCodes.toArray(new String[0]);

        // Phase 1: SQL filtering with location fallback
        List<StructuredClinicalReview> candidates = findCandidatesWithFallback(
                species, symptomCodesArray, locationCountry, locationRegion);

        int totalSearched = candidates.size();
        log.info("Encontrados {} candidatos para matching (especie={}, sintomas={})",
                totalSearched, species, normalizedSymptomCodes);

        // Phase 2: In-memory similarity calculation
        List<MatchResult> matches = new ArrayList<>();
        for (StructuredClinicalReview candidate : candidates) {
            Set<String> historicalSymptoms = extractSymptomCodes(candidate.getSymptomsJson());

            boolean speciesMatch = species != null && species.equalsIgnoreCase(candidate.getSpecies());
            double speciesScore = speciesMatch ? 1.0 : 0.0;
            double symptomJaccard = jaccard(normalizedSymptomCodes, historicalSymptoms);
            double locationScore = calculateLocationScore(locationCountry, locationRegion,
                    candidate.getLocationCountry(), candidate.getLocationRegion());

            Set<String> overlap = new HashSet<>(normalizedSymptomCodes);
            overlap.retainAll(historicalSymptoms);

            double similarity = (speciesScore * 0.3) + (symptomJaccard * 0.5) + (locationScore * 0.2);

            if (similarity >= minSimilarityThreshold) {
                matches.add(new MatchResult(candidate, similarity, speciesMatch, overlap, symptomJaccard, locationScore));
            }
        }

        matches.sort(Comparator.comparingDouble(MatchResult::similarityScore).reversed());
        log.info("{} matches superan el umbral de similaridad {}", matches.size(), minSimilarityThreshold);

        return new MatchingOutput(matches, totalSearched);
    }

    private List<StructuredClinicalReview> findCandidatesWithFallback(
            String species, String[] symptomCodes, String country, String region) {

        // Try with country + region first
        if (country != null && region != null) {
            List<StructuredClinicalReview> results = structuredRepository
                    .findCandidatesBySpeciesAndSymptomsAndCountry(species, symptomCodes, country, maxCandidates);
            if (results.size() >= 10) return results;
        }

        // Fallback: without location filter
        return structuredRepository.findCandidatesBySpeciesAndSymptoms(species, symptomCodes, maxCandidates);
    }

    Set<String> extractSymptomCodes(String symptomsJson) {
        Set<String> codes = new HashSet<>();
        try {
            JsonNode symptoms = objectMapper.readTree(symptomsJson);
            if (symptoms.isArray()) {
                for (JsonNode s : symptoms) {
                    if (s.has("normalized_code") && !s.get("normalized_code").isNull()) {
                        codes.add(s.get("normalized_code").asText());
                    } else if (s.has("suggested_code") && !s.get("suggested_code").isNull()) {
                        codes.add(s.get("suggested_code").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error parseando symptoms_json: {}", e.getMessage());
        }
        return codes;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    static double calculateLocationScore(String clientCountry, String clientRegion,
                                         String vetCountry, String vetRegion) {
        if (clientCountry != null && clientCountry.equalsIgnoreCase(vetCountry)) {
            if (clientRegion != null && clientRegion.equalsIgnoreCase(vetRegion)) {
                return 1.0;
            }
            return 0.5;
        }
        return 0.0;
    }
}
