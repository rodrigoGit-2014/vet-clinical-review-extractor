package com.vetplatform.reviewextractor.domain.enums;

public enum ReviewStatus {
    RECEIVED,
    PROCESSING,
    EXTRACTION_COMPLETED,
    VALIDATION_PASSED,
    VALIDATION_FAILED,
    NORMALIZATION_FAILED,
    COMPLETED,
    FAILED,
    REPROCESSING
}
