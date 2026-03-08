package com.vetplatform.reviewextractor.domain.entity;

import com.vetplatform.reviewextractor.domain.enums.RecommendationStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vet_recommendation_requests")
public class VetRecommendationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_text", nullable = false, columnDefinition = "TEXT")
    private String clientText;

    @Column(name = "pet_owner_id", nullable = false, length = 100)
    private String petOwnerId;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale = "es-CL";

    @Column(name = "location_hint", length = 200)
    private String locationHint;

    @Column(name = "max_results", nullable = false)
    private Integer maxResults = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RecommendationStatus status = RecommendationStatus.RECEIVED;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "llm_provider", length = 30)
    private String llmProvider;

    @Column(name = "llm_model", length = 100)
    private String llmModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interpreted_json", columnDefinition = "jsonb")
    private String interpretedJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_interpretation", columnDefinition = "jsonb")
    private String normalizedInterpretation;

    @Column(name = "interpretation_confidence", precision = 3, scale = 2)
    private BigDecimal interpretationConfidence;

    @Column(name = "processing_duration_ms")
    private Integer processingDurationMs;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "recalculate_count")
    private Integer recalculateCount = 0;

    @Column(name = "failure_reason", length = 50)
    private String failureReason;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_cases_searched")
    private Integer totalCasesSearched;

    @Column(name = "total_matches_found")
    private Integer totalMatchesFound;

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

    public String getClientText() { return clientText; }
    public void setClientText(String clientText) { this.clientText = clientText; }

    public String getPetOwnerId() { return petOwnerId; }
    public void setPetOwnerId(String petOwnerId) { this.petOwnerId = petOwnerId; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getLocationHint() { return locationHint; }
    public void setLocationHint(String locationHint) { this.locationHint = locationHint; }

    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }

    public RecommendationStatus getStatus() { return status; }
    public void setStatus(RecommendationStatus status) { this.status = status; }

    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }

    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }

    public String getInterpretedJson() { return interpretedJson; }
    public void setInterpretedJson(String interpretedJson) { this.interpretedJson = interpretedJson; }

    public String getNormalizedInterpretation() { return normalizedInterpretation; }
    public void setNormalizedInterpretation(String normalizedInterpretation) { this.normalizedInterpretation = normalizedInterpretation; }

    public BigDecimal getInterpretationConfidence() { return interpretationConfidence; }
    public void setInterpretationConfidence(BigDecimal interpretationConfidence) { this.interpretationConfidence = interpretationConfidence; }

    public Integer getProcessingDurationMs() { return processingDurationMs; }
    public void setProcessingDurationMs(Integer processingDurationMs) { this.processingDurationMs = processingDurationMs; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getRecalculateCount() { return recalculateCount; }
    public void setRecalculateCount(Integer recalculateCount) { this.recalculateCount = recalculateCount; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public Integer getTotalCasesSearched() { return totalCasesSearched; }
    public void setTotalCasesSearched(Integer totalCasesSearched) { this.totalCasesSearched = totalCasesSearched; }

    public Integer getTotalMatchesFound() { return totalMatchesFound; }
    public void setTotalMatchesFound(Integer totalMatchesFound) { this.totalMatchesFound = totalMatchesFound; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
