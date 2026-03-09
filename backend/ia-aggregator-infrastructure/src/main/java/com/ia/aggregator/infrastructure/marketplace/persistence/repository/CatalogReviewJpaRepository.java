package com.ia.aggregator.infrastructure.marketplace.persistence.repository;

import com.ia.aggregator.infrastructure.marketplace.persistence.entity.CatalogReviewJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CatalogReviewJpaRepository extends JpaRepository<CatalogReviewJpaEntity, UUID> {

    List<CatalogReviewJpaEntity> findByEntryIdOrderByCreatedAtDesc(UUID entryId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM CatalogReviewJpaEntity r WHERE r.entryId = :entryId")
    double averageRatingByEntryId(@Param("entryId") UUID entryId);
}
