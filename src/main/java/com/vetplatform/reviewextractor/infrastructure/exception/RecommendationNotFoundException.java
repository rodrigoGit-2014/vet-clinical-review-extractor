package com.vetplatform.reviewextractor.infrastructure.exception;

import java.util.UUID;

public class RecommendationNotFoundException extends RuntimeException {

    private final UUID recommendationId;

    public RecommendationNotFoundException(UUID recommendationId) {
        super("Recomendacion no encontrada: " + recommendationId);
        this.recommendationId = recommendationId;
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }
}
