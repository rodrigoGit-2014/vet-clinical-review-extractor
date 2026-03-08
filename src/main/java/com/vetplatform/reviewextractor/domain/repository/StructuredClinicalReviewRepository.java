package com.vetplatform.reviewextractor.domain.repository;

import com.vetplatform.reviewextractor.domain.entity.StructuredClinicalReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StructuredClinicalReviewRepository extends JpaRepository<StructuredClinicalReview, UUID> {

    Optional<StructuredClinicalReview> findTopByReviewIdOrderByVersionDesc(UUID reviewId);

    void deleteByReviewId(UUID reviewId);

    @Query(value = """
        SELECT * FROM structured_clinical_reviews
        WHERE species = :species
        AND vet_name IS NOT NULL
        AND outcome_status IS NOT NULL
        AND EXISTS (
            SELECT 1 FROM jsonb_array_elements(symptoms_json) AS s
            WHERE s->>'normalized_code' = ANY(CAST(:symptomCodes AS text[]))
        )
        ORDER BY created_at DESC
        LIMIT :maxLimit
        """, nativeQuery = true)
    List<StructuredClinicalReview> findCandidatesBySpeciesAndSymptoms(
        @Param("species") String species,
        @Param("symptomCodes") String[] symptomCodes,
        @Param("maxLimit") int maxLimit
    );

    @Query(value = """
        SELECT * FROM structured_clinical_reviews
        WHERE species = :species
        AND vet_name IS NOT NULL
        AND outcome_status IS NOT NULL
        AND location_country = :country
        AND EXISTS (
            SELECT 1 FROM jsonb_array_elements(symptoms_json) AS s
            WHERE s->>'normalized_code' = ANY(CAST(:symptomCodes AS text[]))
        )
        ORDER BY created_at DESC
        LIMIT :maxLimit
        """, nativeQuery = true)
    List<StructuredClinicalReview> findCandidatesBySpeciesAndSymptomsAndCountry(
        @Param("species") String species,
        @Param("symptomCodes") String[] symptomCodes,
        @Param("country") String country,
        @Param("maxLimit") int maxLimit
    );
}
