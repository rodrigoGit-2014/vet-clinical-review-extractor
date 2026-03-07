package com.vetplatform.reviewextractor.infrastructure.exception;

public class ReviewProcessingException extends RuntimeException {

    private final String errorCode;

    public ReviewProcessingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReviewProcessingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
