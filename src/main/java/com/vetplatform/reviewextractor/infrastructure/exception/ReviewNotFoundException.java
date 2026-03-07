package com.vetplatform.reviewextractor.infrastructure.exception;

import java.util.UUID;

public class ReviewNotFoundException extends RuntimeException {

    private final UUID reviewId;

    public ReviewNotFoundException(UUID reviewId) {
        super("Review no encontrado: " + reviewId);
        this.reviewId = reviewId;
    }

    public UUID getReviewId() {
        return reviewId;
    }
}
