package com.vetplatform.reviewextractor.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vet_recommendations")
public class VetRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "vet_name", nullable = false, length = 200)
    private String vetName;

    @Column(name = "vet_clinic", length = 200)
    private String vetClinic;

    @Column(name = "location_country", length = 5)
    private String locationCountry;

    @Column(name = "location_region", length = 100)
    private String locationRegion;

    @Column(name = "score", nullable = false, precision = 4, scale = 3)
    private BigDecimal score;

    @Column(name = "similar_cases_count", nullable = false)
    private Integer similarCasesCount;

    @Column(name = "positive_outcome_rate", precision = 4, scale = 3)
    private BigDecimal positiveOutcomeRate;

    @Column(name = "avg_similarity", precision = 4, scale = 3)
    private BigDecimal avgSimilarity;

    @Column(name = "location_match")
    private Boolean locationMatch = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "explanation_json", nullable = false, columnDefinition = "jsonb")
    private String explanationJson;

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

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }

    public String getVetClinic() { return vetClinic; }
    public void setVetClinic(String vetClinic) { this.vetClinic = vetClinic; }

    public String getLocationCountry() { return locationCountry; }
    public void setLocationCountry(String locationCountry) { this.locationCountry = locationCountry; }

    public String getLocationRegion() { return locationRegion; }
    public void setLocationRegion(String locationRegion) { this.locationRegion = locationRegion; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public Integer getSimilarCasesCount() { return similarCasesCount; }
    public void setSimilarCasesCount(Integer similarCasesCount) { this.similarCasesCount = similarCasesCount; }

    public BigDecimal getPositiveOutcomeRate() { return positiveOutcomeRate; }
    public void setPositiveOutcomeRate(BigDecimal positiveOutcomeRate) { this.positiveOutcomeRate = positiveOutcomeRate; }

    public BigDecimal getAvgSimilarity() { return avgSimilarity; }
    public void setAvgSimilarity(BigDecimal avgSimilarity) { this.avgSimilarity = avgSimilarity; }

    public Boolean getLocationMatch() { return locationMatch; }
    public void setLocationMatch(Boolean locationMatch) { this.locationMatch = locationMatch; }

    public String getExplanationJson() { return explanationJson; }
    public void setExplanationJson(String explanationJson) { this.explanationJson = explanationJson; }

    public Instant getCreatedAt() { return createdAt; }
}
