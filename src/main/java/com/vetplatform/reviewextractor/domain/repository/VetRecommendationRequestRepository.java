package com.vetplatform.reviewextractor.domain.repository;

import com.vetplatform.reviewextractor.domain.entity.VetRecommendationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VetRecommendationRequestRepository extends JpaRepository<VetRecommendationRequest, UUID> {
    List<VetRecommendationRequest> findByPetOwnerIdOrderByCreatedAtDesc(String petOwnerId);
}
