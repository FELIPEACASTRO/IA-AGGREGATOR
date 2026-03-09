package com.ia.aggregator.infrastructure.asset.persistence.repository;

import com.ia.aggregator.infrastructure.asset.persistence.entity.AssetJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetJpaRepository extends JpaRepository<AssetJpaEntity, UUID> {

    List<AssetJpaEntity> findByOrgIdAndScopeAndAssetType(UUID orgId, String scope, String assetType, Pageable pageable);

    List<AssetJpaEntity> findByOrgId(UUID orgId, Pageable pageable);

    @Query("SELECT a FROM AssetJpaEntity a WHERE a.orgId = :orgId AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<AssetJpaEntity> search(@Param("orgId") UUID orgId, @Param("query") String query, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE AssetJpaEntity a SET a.usageCount = a.usageCount + 1 WHERE a.id = :id")
    void incrementUsageCount(@Param("id") UUID id);
}
