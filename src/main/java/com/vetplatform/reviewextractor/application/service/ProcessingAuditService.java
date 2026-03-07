package com.vetplatform.reviewextractor.application.service;

import com.vetplatform.reviewextractor.domain.entity.ReviewProcessingAudit;
import com.vetplatform.reviewextractor.domain.enums.AuditEventType;
import com.vetplatform.reviewextractor.domain.enums.ReviewStatus;
import com.vetplatform.reviewextractor.domain.repository.ReviewProcessingAuditRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProcessingAuditService {

    private final ReviewProcessingAuditRepository auditRepository;

    public ProcessingAuditService(ReviewProcessingAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void logStatusChange(UUID reviewId, UUID correlationId, ReviewStatus from, ReviewStatus to) {
        ReviewProcessingAudit audit = new ReviewProcessingAudit();
        audit.setReviewId(reviewId);
        audit.setCorrelationId(correlationId);
        audit.setEventType(AuditEventType.STATUS_CHANGE);
        audit.setFromStatus(from != null ? from.name() : null);
        audit.setToStatus(to.name());
        auditRepository.save(audit);
    }

    public void logPromptSent(UUID reviewId, UUID correlationId, String promptVersion, String promptText, int attempt) {
        ReviewProcessingAudit audit = new ReviewProcessingAudit();
        audit.setReviewId(reviewId);
        audit.setCorrelationId(correlationId);
        audit.setEventType(AuditEventType.PROMPT_SENT);
        audit.setPromptVersion(promptVersion);
        audit.setPromptText(promptText);
        audit.setAttemptNumber(attempt);
        auditRepository.save(audit);
    }

    public void logLlmResponse(UUID reviewId, UUID correlationId, String rawResponse, int durationMs, int attempt) {
        ReviewProcessingAudit audit = new ReviewProcessingAudit();
        audit.setReviewId(reviewId);
        audit.setCorrelationId(correlationId);
        audit.setEventType(AuditEventType.LLM_RESPONSE);
        audit.setLlmRawResponse(rawResponse);
        audit.setDurationMs(durationMs);
        audit.setAttemptNumber(attempt);
        auditRepository.save(audit);
    }

    public void logValidationResult(UUID reviewId, UUID correlationId, String detailsJson) {
        ReviewProcessingAudit audit = new ReviewProcessingAudit();
        audit.setReviewId(reviewId);
        audit.setCorrelationId(correlationId);
        audit.setEventType(AuditEventType.VALIDATION_RESULT);
        audit.setDetailsJson(detailsJson);
        auditRepository.save(audit);
    }

    public void logNormalizationResult(UUID reviewId, UUID correlationId, String detailsJson) {
        ReviewProcessingAudit audit = new ReviewProcessingAudit();
        audit.setReviewId(reviewId);
        audit.setCorrelationId(correlationId);
        audit.setEventType(AuditEventType.NORMALIZATION_RESULT);
        audit.setDetailsJson(detailsJson);
        auditRepository.save(audit);
    }

    public void logError(UUID reviewId, UUID correlationId, String errorCode, String errorMessage, int attempt) {
        ReviewProcessingAudit audit = new ReviewProcessingAudit();
        audit.setReviewId(reviewId);
        audit.setCorrelationId(correlationId);
        audit.setEventType(AuditEventType.ERROR);
        audit.setErrorCode(errorCode);
        audit.setErrorMessage(errorMessage);
        audit.setAttemptNumber(attempt);
        auditRepository.save(audit);
    }
}
