package com.vetplatform.reviewextractor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetplatform.reviewextractor.application.service.CaseMatchingService;
import com.vetplatform.reviewextractor.application.service.CaseMatchingService.MatchResult;
import com.vetplatform.reviewextractor.application.service.CaseMatchingService.MatchingOutput;
import com.vetplatform.reviewextractor.domain.entity.StructuredClinicalReview;
import com.vetplatform.reviewextractor.domain.repository.StructuredClinicalReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseMatchingServiceTest {

    @Mock
    private StructuredClinicalReviewRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CaseMatchingService createService() {
        return new CaseMatchingService(repository, objectMapper, 0.3, 500);
    }

    // --- Reflection helpers for package-private/static methods ---

    private static double invokeJaccard(Set<String> a, Set<String> b) throws Exception {
        Method method = CaseMatchingService.class.getDeclaredMethod("jaccard", Set.class, Set.class);
        method.setAccessible(true);
        return (double) method.invoke(null, a, b);
    }

    private static double invokeCalculateLocationScore(String clientCountry, String clientRegion,
                                                       String vetCountry, String vetRegion) throws Exception {
        Method method = CaseMatchingService.class.getDeclaredMethod(
                "calculateLocationScore", String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        return (double) method.invoke(null, clientCountry, clientRegion, vetCountry, vetRegion);
    }

    @SuppressWarnings("unchecked")
    private Set<String> invokeExtractSymptomCodes(CaseMatchingService service, String json) throws Exception {
        Method method = CaseMatchingService.class.getDeclaredMethod("extractSymptomCodes", String.class);
        method.setAccessible(true);
        return (Set<String>) method.invoke(service, json);
    }

    // --- jaccard tests ---

    @Test
    void shouldCalculateJaccardCorrectly() throws Exception {
        Set<String> a = Set.of("A", "B");
        Set<String> b = Set.of("B", "C");
        double result = invokeJaccard(a, b);
        assertEquals(1.0 / 3.0, result, 0.0001);
    }

    @Test
    void shouldReturnZeroJaccardForDisjointSets() throws Exception {
        Set<String> a = Set.of("A");
        Set<String> b = Set.of("B");
        double result = invokeJaccard(a, b);
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    void shouldReturnOneJaccardForIdenticalSets() throws Exception {
        Set<String> a = Set.of("A", "B");
        Set<String> b = Set.of("A", "B");
        double result = invokeJaccard(a, b);
        assertEquals(1.0, result, 0.0001);
    }

    @Test
    void shouldReturnZeroJaccardForEmptySets() throws Exception {
        Set<String> a = Set.of();
        Set<String> b = Set.of();
        double result = invokeJaccard(a, b);
        assertEquals(0.0, result, 0.0001);
    }

    // --- calculateLocationScore tests ---

    @Test
    void shouldCalculateLocationScoreCountryAndRegion() throws Exception {
        double score = invokeCalculateLocationScore("CL", "Maule", "CL", "Maule");
        assertEquals(1.0, score, 0.0001);
    }

    @Test
    void shouldCalculateLocationScoreCountryOnly() throws Exception {
        double score = invokeCalculateLocationScore("CL", "Maule", "CL", "Biobio");
        assertEquals(0.5, score, 0.0001);
    }

    @Test
    void shouldCalculateLocationScoreNoMatch() throws Exception {
        double score = invokeCalculateLocationScore("CL", "Maule", "AR", "Buenos Aires");
        assertEquals(0.0, score, 0.0001);
    }

    // --- extractSymptomCodes tests ---

    @Test
    void shouldExtractNormalizedCodes() throws Exception {
        CaseMatchingService service = createService();
        String json = """
                [
                  {"description": "vomitos", "normalized_code": "VOMITING", "suggested_code": "VOMIT_RAW"},
                  {"description": "diarrea", "normalized_code": "DIARRHEA", "suggested_code": "DIARR_RAW"}
                ]
                """;
        Set<String> codes = invokeExtractSymptomCodes(service, json);
        assertEquals(Set.of("VOMITING", "DIARRHEA"), codes);
    }

    @Test
    void shouldExtractSuggestedCodeAsFallback() throws Exception {
        CaseMatchingService service = createService();
        String json = """
                [
                  {"description": "fiebre", "suggested_code": "FEVER"},
                  {"description": "tos", "normalized_code": "COUGH", "suggested_code": "COUGH_RAW"}
                ]
                """;
        Set<String> codes = invokeExtractSymptomCodes(service, json);
        assertEquals(Set.of("FEVER", "COUGH"), codes);
    }

    // --- findSimilarCases tests ---

    @Test
    void shouldReturnEmptyWhenNoSymptoms() {
        CaseMatchingService service = createService();
        MatchingOutput output = service.findSimilarCases("DOG", Set.of(), "CL", "Maule");
        assertTrue(output.matches().isEmpty());
        assertEquals(0, output.totalCasesSearched());
    }

    @Test
    void shouldFilterBySimilarityThreshold() {
        CaseMatchingService service = createService();

        // Candidate: different species (CAT vs DOG query), no symptom overlap, different country
        // speciesMatch=false(0.0), jaccard({VOMITING},{SEIZURES})=0, location(CL vs AR)=0
        // similarity = 0.0 -> below 0.3
        StructuredClinicalReview belowThreshold = buildReview(
                "Dr. Low", "CAT", "WORSENING",
                "[{\"normalized_code\": \"SEIZURES\"}]",
                "AR", "Buenos Aires", "Clinica AR"
        );

        when(repository.findCandidatesBySpeciesAndSymptomsAndCountry(
                eq("DOG"), any(String[].class), eq("CL"), eq(500)))
                .thenReturn(List.of());
        when(repository.findCandidatesBySpeciesAndSymptoms(
                eq("DOG"), any(String[].class), eq(500)))
                .thenReturn(List.of(belowThreshold));

        MatchingOutput output = service.findSimilarCases("DOG", Set.of("VOMITING"), "CL", "Maule");
        assertTrue(output.matches().isEmpty());
        assertEquals(1, output.totalCasesSearched());
    }

    @Test
    void shouldFindSimilarCasesWithMatches() {
        CaseMatchingService service = createService();

        // Good candidate: same species (DOG), overlapping symptoms, same location
        StructuredClinicalReview goodCandidate = buildReview(
                "Dr. Perez", "DOG", "FULLY_RECOVERED",
                "[{\"normalized_code\": \"VOMITING\"}, {\"normalized_code\": \"DIARRHEA\"}]",
                "CL", "Maule", "Clinica Maule"
        );

        // Query: DOG, symptoms={VOMITING, FEVER}, location=CL/Maule
        // speciesMatch = true -> 1.0
        // jaccard({VOMITING,FEVER}, {VOMITING,DIARRHEA}) = 1/3
        // locationScore(CL,Maule,CL,Maule) = 1.0
        // similarity = (1.0*0.3) + (0.333*0.5) + (1.0*0.2) = 0.3 + 0.1667 + 0.2 = 0.6667

        // Return enough candidates (>=10) from the country-scoped query to avoid fallback
        List<StructuredClinicalReview> candidates = new ArrayList<>();
        candidates.add(goodCandidate);
        // Add 9 filler candidates with different species and no symptom overlap (similarity=0, filtered out)
        for (int i = 0; i < 9; i++) {
            candidates.add(buildReview(
                    "Dr. Filler" + i, "CAT", "UNKNOWN",
                    "[{\"normalized_code\": \"UNRELATED_" + i + "\"}]",
                    "AR", "Other", "Filler Clinic"
            ));
        }

        when(repository.findCandidatesBySpeciesAndSymptomsAndCountry(
                eq("DOG"), any(String[].class), eq("CL"), eq(500)))
                .thenReturn(candidates);

        Set<String> symptoms = new LinkedHashSet<>(List.of("VOMITING", "FEVER"));
        MatchingOutput output = service.findSimilarCases("DOG", symptoms, "CL", "Maule");

        assertEquals(1, output.matches().size());
        MatchResult match = output.matches().getFirst();
        assertTrue(match.speciesMatch());
        assertEquals(Set.of("VOMITING"), match.overlappingSymptoms());

        double expectedJaccard = 1.0 / 3.0;
        assertEquals(expectedJaccard, match.symptomJaccard(), 0.0001);

        double expectedSimilarity = (1.0 * 0.3) + (expectedJaccard * 0.5) + (1.0 * 0.2);
        assertEquals(expectedSimilarity, match.similarityScore(), 0.0001);
        assertEquals(1.0, match.locationScore(), 0.0001);
    }

    // --- helpers ---

    private StructuredClinicalReview buildReview(String vetName, String species, String outcome,
                                                  String symptomsJson, String country,
                                                  String region, String clinic) {
        StructuredClinicalReview review = new StructuredClinicalReview();
        review.setId(UUID.randomUUID());
        review.setVetName(vetName);
        review.setSpecies(species);
        review.setOutcomeStatus(outcome);
        review.setSymptomsJson(symptomsJson);
        review.setLocationCountry(country);
        review.setLocationRegion(region);
        review.setVetClinic(clinic);
        return review;
    }
}
