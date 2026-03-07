package com.vetplatform.reviewextractor.domain.repository;

import com.vetplatform.reviewextractor.domain.entity.NormalizationSynonym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NormalizationSynonymRepository extends JpaRepository<NormalizationSynonym, Integer> {

    Optional<NormalizationSynonym> findByCategoryAndRawTermAndActiveTrue(String category, String rawTerm);

    List<NormalizationSynonym> findByCategoryAndActiveTrue(String category);

    List<NormalizationSynonym> findByActiveTrue();
}
