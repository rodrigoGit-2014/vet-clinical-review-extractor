package com.vetplatform.reviewextractor.domain.repository;

import com.vetplatform.reviewextractor.domain.entity.ClinicalReview;
import com.vetplatform.reviewextractor.domain.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClinicalReviewRepository extends JpaRepository<ClinicalReview, UUID> {

    List<ClinicalReview> findByPetOwnerIdOrderByCreatedAtDesc(String petOwnerId);

    List<ClinicalReview> findByStatus(ReviewStatus status);
}
