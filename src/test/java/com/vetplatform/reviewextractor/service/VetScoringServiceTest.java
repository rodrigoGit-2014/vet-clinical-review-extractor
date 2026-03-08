package com.vetplatform.reviewextractor.service;

import com.vetplatform.reviewextractor.application.service.CaseMatchingService.MatchResult;
import com.vetplatform.reviewextractor.application.service.VetScoringService;
import com.vetplatform.reviewextractor.application.service.VetScoringService.ScoredVet;
import com.vetplatform.reviewextractor.domain.entity.StructuredClinicalReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VetScoringServiceTest {

    private VetScoringService service;

    @BeforeEach
    void setUp() {
        service = new VetScoringService(2);
    }

    @Test
    void shouldReturnEmptyForNoMatches() {
        List<ScoredVet> result = service.scoreAndRank(List.of(), 10);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldScoreAndRankVets() {
        // Vet A: 2 cases, high similarity, all positive, location match
        MatchResult a1 = buildMatch("Dr. Alpha", "Clinica A", "DOG", "FULLY_RECOVERED",
                0.8, true, Set.of("VOMITING"), 0.6, 1.0, "CL", "Maule");
        MatchResult a2 = buildMatch("Dr. Alpha", "Clinica A", "DOG", "IMPROVING",
                0.7, true, Set.of("VOMITING", "DIARRHEA"), 0.5, 1.0, "CL", "Maule");

        // Vet B: 2 cases, lower similarity, mixed outcomes, no location
        MatchResult b1 = buildMatch("Dr. Beta", "Clinica B", "DOG", "WORSENING",
                0.5, true, Set.of("FEVER"), 0.3, 0.0, "AR", "Buenos Aires");
        MatchResult b2 = buildMatch("Dr. Beta", "Clinica B", "DOG", "STABLE",
                0.4, true, Set.of("FEVER"), 0.3, 0.0, "AR", "Buenos Aires");

        List<ScoredVet> result = service.scoreAndRank(List.of(a1, a2, b1, b2), 10);

        assertEquals(2, result.size());
        assertEquals("Dr. Alpha", result.get(0).vetName());
        assertEquals("Dr. Beta", result.get(1).vetName());
        assertTrue(result.get(0).score() > result.get(1).score());
    }

    @Test
    void shouldLimitResultsByMaxResults() {
        // 3 vets, each with 2 cases
        List<MatchResult> matches = new ArrayList<>();
        for (String vet : List.of("Dr. A", "Dr. B", "Dr. C")) {
            matches.add(buildMatch(vet, "Clinic", "DOG", "FULLY_RECOVERED",
                    0.7, true, Set.of("VOMITING"), 0.5, 0.0, "CL", "Maule"));
            matches.add(buildMatch(vet, "Clinic", "DOG", "IMPROVING",
                    0.6, true, Set.of("VOMITING"), 0.4, 0.0, "CL", "Maule"));
        }

        List<ScoredVet> result = service.scoreAndRank(matches, 2);
        assertEquals(2, result.size());
    }

    @Test
    void shouldFallbackToMinOneCaseWhenFewVets() {
        // Only 1 vet with 1 case -- fewer than 2 vets meet minCases=2, so fallback to 1
        MatchResult single = buildMatch("Dr. Solo", "Clinica Solo", "CAT", "FULLY_RECOVERED",
                0.8, true, Set.of("COUGH"), 0.5, 0.0, "CL", "Santiago");

        List<ScoredVet> result = service.scoreAndRank(List.of(single), 10);

        assertEquals(1, result.size());
        assertEquals("Dr. Solo", result.getFirst().vetName());
        assertEquals(1, result.getFirst().similarCasesCount());
    }

    @Test
    void shouldCalculatePositiveOutcomeRate() {
        // 4 cases: FULLY_RECOVERED, IMPROVING, STABLE, WORSENING -> 3/4 = 0.75
        MatchResult m1 = buildMatch("Dr. Mixed", "Clinic", "DOG", "FULLY_RECOVERED",
                0.6, true, Set.of("VOMITING"), 0.4, 0.0, "CL", "Maule");
        MatchResult m2 = buildMatch("Dr. Mixed", "Clinic", "DOG", "IMPROVING",
                0.6, true, Set.of("VOMITING"), 0.4, 0.0, "CL", "Maule");
        MatchResult m3 = buildMatch("Dr. Mixed", "Clinic", "DOG", "STABLE",
                0.6, true, Set.of("VOMITING"), 0.4, 0.0, "CL", "Maule");
        MatchResult m4 = buildMatch("Dr. Mixed", "Clinic", "DOG", "WORSENING",
                0.6, true, Set.of("VOMITING"), 0.4, 0.0, "CL", "Maule");

        List<ScoredVet> result = service.scoreAndRank(List.of(m1, m2, m3, m4), 10);

        assertEquals(1, result.size());
        assertEquals(0.75, result.getFirst().positiveOutcomeRate(), 0.0001);
    }

    @Test
    void shouldGenerateSummaryInSpanish() {
        MatchResult m1 = buildMatch("Dr. Perez", "Clinica Sur", "DOG", "FULLY_RECOVERED",
                0.7, true, Set.of("VOMITING"), 0.5, 0.0, "CL", "Maule");
        MatchResult m2 = buildMatch("Dr. Perez", "Clinica Sur", "DOG", "IMPROVING",
                0.6, true, Set.of("DIARRHEA"), 0.4, 0.0, "CL", "Maule");

        List<ScoredVet> result = service.scoreAndRank(List.of(m1, m2), 10);

        assertEquals(1, result.size());
        String summary = result.getFirst().summary();
        assertTrue(summary.contains("Dr. Perez"), "Summary should contain vet name");
        assertTrue(summary.contains("casos similares"), "Summary should contain 'casos similares'");
    }

    @Test
    void shouldExcludeVetsWithNullName() {
        MatchResult nullName = buildMatch(null, "Clinica X", "DOG", "FULLY_RECOVERED",
                0.8, true, Set.of("VOMITING"), 0.5, 0.0, "CL", "Maule");
        MatchResult named = buildMatch("Dr. Valid", "Clinica Y", "DOG", "FULLY_RECOVERED",
                0.8, true, Set.of("VOMITING"), 0.5, 0.0, "CL", "Maule");

        // With only 1 named vet (1 case), fallback to minCases=1
        List<ScoredVet> result = service.scoreAndRank(List.of(nullName, named), 10);

        assertEquals(1, result.size());
        assertEquals("Dr. Valid", result.getFirst().vetName());
    }

    @Test
    void shouldHandleLocationBonus() {
        // Vet with location match (locationScore > 0)
        MatchResult withLocation = buildMatch("Dr. Local", "Clinic Local", "DOG", "FULLY_RECOVERED",
                0.6, true, Set.of("VOMITING"), 0.4, 1.0, "CL", "Maule");
        MatchResult withLocation2 = buildMatch("Dr. Local", "Clinic Local", "DOG", "IMPROVING",
                0.6, true, Set.of("VOMITING"), 0.4, 0.5, "CL", "Biobio");

        // Vet without location match (locationScore = 0)
        MatchResult noLocation = buildMatch("Dr. Remote", "Clinic Remote", "DOG", "FULLY_RECOVERED",
                0.6, true, Set.of("VOMITING"), 0.4, 0.0, "AR", "Buenos Aires");
        MatchResult noLocation2 = buildMatch("Dr. Remote", "Clinic Remote", "DOG", "IMPROVING",
                0.6, true, Set.of("VOMITING"), 0.4, 0.0, "AR", "Buenos Aires");

        List<ScoredVet> result = service.scoreAndRank(
                List.of(withLocation, withLocation2, noLocation, noLocation2), 10);

        assertEquals(2, result.size());

        ScoredVet local = result.stream()
                .filter(v -> "Dr. Local".equals(v.vetName())).findFirst().orElseThrow();
        ScoredVet remote = result.stream()
                .filter(v -> "Dr. Remote".equals(v.vetName())).findFirst().orElseThrow();

        assertTrue(local.locationMatch());
        assertFalse(remote.locationMatch());
        // Local vet gets locationBonus=0.1*0.10=0.01 extra
        assertTrue(local.score() > remote.score(),
                "Vet with location match should have higher score");
    }

    // --- helpers ---

    private MatchResult buildMatch(String vetName, String clinic, String species, String outcome,
                                   double similarityScore, boolean speciesMatch,
                                   Set<String> overlappingSymptoms, double symptomJaccard,
                                   double locationScore, String country, String region) {
        StructuredClinicalReview review = new StructuredClinicalReview();
        review.setId(UUID.randomUUID());
        review.setVetName(vetName);
        review.setVetClinic(clinic);
        review.setSpecies(species);
        review.setOutcomeStatus(outcome);
        review.setSymptomsJson("[]");
        review.setLocationCountry(country);
        review.setLocationRegion(region);
        return new MatchResult(review, similarityScore, speciesMatch,
                overlappingSymptoms, symptomJaccard, locationScore);
    }
}
