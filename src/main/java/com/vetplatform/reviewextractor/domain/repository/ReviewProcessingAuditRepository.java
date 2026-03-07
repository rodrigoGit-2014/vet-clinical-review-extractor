package com.vetplatform.reviewextractor.domain.repository;

import com.vetplatform.reviewextractor.domain.entity.ReviewProcessingAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewProcessingAuditRepository extends JpaRepository<ReviewProcessingAudit, Long> {

    List<ReviewProcessingAudit> findByReviewIdOrderByCreatedAtAsc(UUID reviewId);

    List<ReviewProcessingAudit> findByCorrelationIdOrderByCreatedAtAsc(UUID correlationId);
}
