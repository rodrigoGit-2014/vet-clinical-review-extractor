package com.vetplatform.reviewextractor.domain.repository;

import com.vetplatform.reviewextractor.domain.entity.VetRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VetRecommendationRepository extends JpaRepository<VetRecommendation, UUID> {
    List<VetRecommendation> findByRequestIdOrderByRankPositionAsc(UUID requestId);
    void deleteByRequestId(UUID requestId);
}
