package com.vetplatform.reviewextractor.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "normalization_synonyms")
public class NormalizationSynonym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "raw_term", nullable = false, length = 200)
    private String rawTerm;

    @Column(name = "normalized_code", nullable = false, length = 100)
    private String normalizedCode;

    @Column(name = "normalized_label", nullable = false, length = 200)
    private String normalizedLabel;

    @Column(name = "locale", length = 10)
    private String locale = "es";

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Integer getId() { return id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRawTerm() { return rawTerm; }
    public void setRawTerm(String rawTerm) { this.rawTerm = rawTerm; }

    public String getNormalizedCode() { return normalizedCode; }
    public void setNormalizedCode(String normalizedCode) { this.normalizedCode = normalizedCode; }

    public String getNormalizedLabel() { return normalizedLabel; }
    public void setNormalizedLabel(String normalizedLabel) { this.normalizedLabel = normalizedLabel; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
}
