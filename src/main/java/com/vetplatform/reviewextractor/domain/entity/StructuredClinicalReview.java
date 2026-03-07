package com.vetplatform.reviewextractor.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "structured_clinical_reviews")
public class StructuredClinicalReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "species", length = 30)
    private String species;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "pet_name", length = 100)
    private String petName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "symptoms_json", nullable = false, columnDefinition = "jsonb")
    private String symptomsJson = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "procedures_json", nullable = false, columnDefinition = "jsonb")
    private String proceduresJson = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medications_json", nullable = false, columnDefinition = "jsonb")
    private String medicationsJson = "[]";

    @Column(name = "vet_name", length = 200)
    private String vetName;

    @Column(name = "vet_clinic", length = 200)
    private String vetClinic;

    @Column(name = "location_raw", length = 200)
    private String locationRaw;

    @Column(name = "location_normalized", length = 200)
    private String locationNormalized;

    @Column(name = "location_region", length = 100)
    private String locationRegion;

    @Column(name = "location_country", length = 5)
    private String locationCountry;

    @Column(name = "outcome_status", length = 30)
    private String outcomeStatus;

    @Column(name = "outcome_description", columnDefinition = "TEXT")
    private String outcomeDescription;

    @Column(name = "overall_confidence", precision = 3, scale = 2)
    private BigDecimal overallConfidence;

    @Column(name = "extraction_notes", columnDefinition = "TEXT")
    private String extractionNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getReviewId() { return reviewId; }
    public void setReviewId(UUID reviewId) { this.reviewId = reviewId; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getSymptomsJson() { return symptomsJson; }
    public void setSymptomsJson(String symptomsJson) { this.symptomsJson = symptomsJson; }

    public String getProceduresJson() { return proceduresJson; }
    public void setProceduresJson(String proceduresJson) { this.proceduresJson = proceduresJson; }

    public String getMedicationsJson() { return medicationsJson; }
    public void setMedicationsJson(String medicationsJson) { this.medicationsJson = medicationsJson; }

    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }

    public String getVetClinic() { return vetClinic; }
    public void setVetClinic(String vetClinic) { this.vetClinic = vetClinic; }

    public String getLocationRaw() { return locationRaw; }
    public void setLocationRaw(String locationRaw) { this.locationRaw = locationRaw; }

    public String getLocationNormalized() { return locationNormalized; }
    public void setLocationNormalized(String locationNormalized) { this.locationNormalized = locationNormalized; }

    public String getLocationRegion() { return locationRegion; }
    public void setLocationRegion(String locationRegion) { this.locationRegion = locationRegion; }

    public String getLocationCountry() { return locationCountry; }
    public void setLocationCountry(String locationCountry) { this.locationCountry = locationCountry; }

    public String getOutcomeStatus() { return outcomeStatus; }
    public void setOutcomeStatus(String outcomeStatus) { this.outcomeStatus = outcomeStatus; }

    public String getOutcomeDescription() { return outcomeDescription; }
    public void setOutcomeDescription(String outcomeDescription) { this.outcomeDescription = outcomeDescription; }

    public BigDecimal getOverallConfidence() { return overallConfidence; }
    public void setOverallConfidence(BigDecimal overallConfidence) { this.overallConfidence = overallConfidence; }

    public String getExtractionNotes() { return extractionNotes; }
    public void setExtractionNotes(String extractionNotes) { this.extractionNotes = extractionNotes; }

    public Instant getCreatedAt() { return createdAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
