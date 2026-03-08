package com.vetplatform.reviewextractor.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recommendation_matches")
public class RecommendationMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "structured_review_id", nullable = false)
    private UUID structuredReviewId;

    @Column(name = "vet_name", nullable = false, length = 200)
    private String vetName;

    @Column(name = "similarity_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal similarityScore;

    @Column(name = "species_match", nullable = false)
    private Boolean speciesMatch;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "symptom_overlap_codes", nullable = false, columnDefinition = "jsonb")
    private String symptomOverlapCodes;

    @Column(name = "symptom_jaccard", precision = 4, scale = 3)
    private BigDecimal symptomJaccard;

    @Column(name = "location_score", precision = 4, scale = 3)
    private BigDecimal locationScore;

    @Column(name = "outcome_status", length = 30)
    private String outcomeStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public UUID getStructuredReviewId() { return structuredReviewId; }
    public void setStructuredReviewId(UUID structuredReviewId) { this.structuredReviewId = structuredReviewId; }

    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }

    public BigDecimal getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(BigDecimal similarityScore) { this.similarityScore = similarityScore; }

    public Boolean getSpeciesMatch() { return speciesMatch; }
    public void setSpeciesMatch(Boolean speciesMatch) { this.speciesMatch = speciesMatch; }

    public String getSymptomOverlapCodes() { return symptomOverlapCodes; }
    public void setSymptomOverlapCodes(String symptomOverlapCodes) { this.symptomOverlapCodes = symptomOverlapCodes; }

    public BigDecimal getSymptomJaccard() { return symptomJaccard; }
    public void setSymptomJaccard(BigDecimal symptomJaccard) { this.symptomJaccard = symptomJaccard; }

    public BigDecimal getLocationScore() { return locationScore; }
    public void setLocationScore(BigDecimal locationScore) { this.locationScore = locationScore; }

    public String getOutcomeStatus() { return outcomeStatus; }
    public void setOutcomeStatus(String outcomeStatus) { this.outcomeStatus = outcomeStatus; }

    public Instant getCreatedAt() { return createdAt; }
}
