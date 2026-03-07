package com.vetplatform.reviewextractor.domain.entity;

import com.vetplatform.reviewextractor.domain.enums.ReviewStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinical_reviews")
public class ClinicalReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "pet_owner_id", nullable = false, length = 100)
    private String petOwnerId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale = "es-CL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReviewStatus status = ReviewStatus.RECEIVED;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "llm_provider", length = 30)
    private String llmProvider;

    @Column(name = "llm_model", length = 100)
    private String llmModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_json", columnDefinition = "jsonb")
    private String extractedJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_json", columnDefinition = "jsonb")
    private String normalizedJson;

    @Column(name = "overall_confidence", precision = 3, scale = 2)
    private BigDecimal overallConfidence;

    @Column(name = "processing_duration_ms")
    private Integer processingDurationMs;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "reprocess_count")
    private Integer reprocessCount = 0;

    @Column(name = "failure_reason", length = 50)
    private String failureReason;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getPetOwnerId() { return petOwnerId; }
    public void setPetOwnerId(String petOwnerId) { this.petOwnerId = petOwnerId; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }

    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }

    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }

    public String getExtractedJson() { return extractedJson; }
    public void setExtractedJson(String extractedJson) { this.extractedJson = extractedJson; }

    public String getNormalizedJson() { return normalizedJson; }
    public void setNormalizedJson(String normalizedJson) { this.normalizedJson = normalizedJson; }

    public BigDecimal getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(BigDecimal overallConfidence) { this.overallConfidence = overallConfidence; }

    public Integer getProcessingDurationMs() { return processingDurationMs; }
    public void setProcessingDurationMs(Integer processingDurationMs) { this.processingDurationMs = processingDurationMs; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getReprocessCount() { return reprocessCount; }
    public void setReprocessCount(Integer reprocessCount) { this.reprocessCount = reprocessCount; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
