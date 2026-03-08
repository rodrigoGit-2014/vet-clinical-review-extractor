package com.vetplatform.reviewextractor.domain.repository;

import com.vetplatform.reviewextractor.domain.entity.RecommendationMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendationMatchRepository extends JpaRepository<RecommendationMatch, UUID> {
    List<RecommendationMatch> findByRequestId(UUID requestId);
    void deleteByRequestId(UUID requestId);
}
