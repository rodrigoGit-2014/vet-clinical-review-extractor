package com.vetplatform.reviewextractor.infrastructure.exception;

public class LlmInvocationException extends RuntimeException {

    public LlmInvocationException(String message) {
        super(message);
    }

    public LlmInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
