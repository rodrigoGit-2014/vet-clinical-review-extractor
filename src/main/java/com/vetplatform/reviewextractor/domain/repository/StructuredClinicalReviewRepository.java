package com.vetplatform.reviewextractor.domain.repository;

import com.vetplatform.reviewextractor.domain.entity.StructuredClinicalReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StructuredClinicalReviewRepository extends JpaRepository<StructuredClinicalReview, UUID> {

    Optional<StructuredClinicalReview> findTopByReviewIdOrderByVersionDesc(UUID reviewId);

    void deleteByReviewId(UUID reviewId);
}
