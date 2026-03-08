package com.vetplatform.reviewextractor.infrastructure.exception;

import java.util.UUID;

public class RecommendationAlreadyProcessingException extends RuntimeException {

    private final UUID recommendationId;

    public RecommendationAlreadyProcessingException(UUID recommendationId) {
        super("Recomendacion ya esta en procesamiento: " + recommendationId);
        this.recommendationId = recommendationId;
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }
}
