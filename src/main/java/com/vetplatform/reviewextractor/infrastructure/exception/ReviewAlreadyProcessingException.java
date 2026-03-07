package com.vetplatform.reviewextractor.infrastructure.exception;

import java.util.UUID;

public class ReviewAlreadyProcessingException extends RuntimeException {

    private final UUID reviewId;

    public ReviewAlreadyProcessingException(UUID reviewId) {
        super("Review ya esta en procesamiento: " + reviewId);
        this.reviewId = reviewId;
    }

    public UUID getReviewId() {
        return reviewId;
    }
}
