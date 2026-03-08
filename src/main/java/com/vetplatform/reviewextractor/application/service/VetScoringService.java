package com.vetplatform.reviewextractor.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VetScoringService {

    private final int minCasesPerVet;

    public VetScoringService(@Value("${matching.min-cases-per-vet:2}") int minCasesPerVet) {
        this.minCasesPerVet = minCasesPerVet;
    }

    public record ScoredVet(
            String vetName,
            String vetClinic,
            String locationCountry,
            String locationRegion,
            double score,
            int similarCasesCount,
            double positiveOutcomeRate,
            double avgSimilarity,
            boolean locationMatch,
            List<String> matchedSymptoms,
            Map<String, Integer> outcomeBreakdown,
            String summary
    ) {}

    public List<ScoredVet> scoreAndRank(List<CaseMatchingService.MatchResult> matches, int maxResults) {
        // Group by vet_name (case-insensitive)
        Map<String, List<CaseMatchingService.MatchResult>> byVet = matches.stream()
                .filter(m -> m.review().getVetName() != null)
                .collect(Collectors.groupingBy(
                        m -> m.review().getVetName().toLowerCase().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int effectiveMinCases = minCasesPerVet;
        long vetsWithMinCases = byVet.values().stream().filter(v -> v.size() >= minCasesPerVet).count();
        if (vetsWithMinCases < 2 && !byVet.isEmpty()) {
            effectiveMinCases = 1; // fallback
        }

        List<ScoredVet> scoredVets = new ArrayList<>();
        for (var entry : byVet.entrySet()) {
            List<CaseMatchingService.MatchResult> vetMatches = entry.getValue();
            if (vetMatches.size() < effectiveMinCases) continue;

            CaseMatchingService.MatchResult first = vetMatches.getFirst();
            String vetName = first.review().getVetName();
            String vetClinic = first.review().getVetClinic();
            String locCountry = first.review().getLocationCountry();
            String locRegion = first.review().getLocationRegion();

            double avgSimilarity = vetMatches.stream().mapToDouble(CaseMatchingService.MatchResult::similarityScore).average().orElse(0.0);
            boolean hasLocationMatch = vetMatches.stream().anyMatch(m -> m.locationScore() > 0.0);
            double locationBonus = hasLocationMatch ? 0.1 : 0.0;

            // Outcome analysis
            Map<String, Integer> outcomeBreakdown = new LinkedHashMap<>();
            int positiveCount = 0;
            for (var m : vetMatches) {
                String outcome = m.review().getOutcomeStatus();
                if (outcome != null) {
                    outcomeBreakdown.merge(outcome, 1, Integer::sum);
                    if (isPositiveOutcome(outcome)) positiveCount++;
                }
            }
            double positiveRate = (double) positiveCount / vetMatches.size();

            // Volume score: logarithmic, capped at 10
            double volumeScore = Math.min(Math.log10(vetMatches.size() + 1) / Math.log10(11), 1.0);

            double score = (avgSimilarity * 0.40) + (positiveRate * 0.30) + (volumeScore * 0.20) + (locationBonus * 0.10);
            score = Math.round(score * 1000.0) / 1000.0;

            // Collect matched symptoms
            Set<String> allMatchedSymptoms = new LinkedHashSet<>();
            for (var m : vetMatches) {
                allMatchedSymptoms.addAll(m.overlappingSymptoms());
            }

            // Build summary
            String summary = buildSummary(vetName, vetMatches.size(), positiveRate, new ArrayList<>(allMatchedSymptoms), hasLocationMatch);

            scoredVets.add(new ScoredVet(
                    vetName, vetClinic, locCountry, locRegion,
                    score, vetMatches.size(), positiveRate, avgSimilarity,
                    hasLocationMatch, new ArrayList<>(allMatchedSymptoms),
                    outcomeBreakdown, summary
            ));
        }

        scoredVets.sort(Comparator.comparingDouble(ScoredVet::score).reversed());
        return scoredVets.stream().limit(maxResults).toList();
    }

    private boolean isPositiveOutcome(String status) {
        return "FULLY_RECOVERED".equals(status) || "IMPROVING".equals(status) || "STABLE".equals(status);
    }

    private String buildSummary(String vetName, int caseCount, double positiveRate,
                                List<String> matchedSymptoms, boolean locationMatch) {
        StringBuilder sb = new StringBuilder();
        sb.append(vetName).append(" ha tratado ").append(caseCount);
        sb.append(caseCount == 1 ? " caso similar" : " casos similares");

        if (!matchedSymptoms.isEmpty()) {
            sb.append(" relacionados con ");
            sb.append(String.join(", ", matchedSymptoms.stream()
                    .map(this::codeToLabel).toList()));
        }

        sb.append(", con un ").append(String.format("%.0f", positiveRate * 100))
                .append("% de resultados positivos");

        if (locationMatch) {
            sb.append(" en tu misma zona");
        }
        sb.append(".");

        return sb.toString();
    }

    private String codeToLabel(String code) {
        return switch (code) {
            case "APPETITE_LOSS" -> "perdida de apetito";
            case "VOMITING" -> "vomitos";
            case "DIARRHEA" -> "diarrea";
            case "LETHARGY" -> "decaimiento";
            case "FEVER" -> "fiebre";
            case "COUGH" -> "tos";
            case "MASS" -> "masa/bulto";
            case "HEMORRHAGE" -> "sangrado";
            case "PAIN" -> "dolor";
            case "PRURITUS" -> "picazon";
            case "INFLAMMATION" -> "inflamacion";
            case "SEIZURES" -> "convulsiones";
            case "LAMENESS" -> "cojera";
            default -> code.toLowerCase().replace("_", " ");
        };
    }
}
