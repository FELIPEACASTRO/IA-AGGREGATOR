package com.ia.aggregator.infrastructure.marketplace.persistence.repository;

import com.ia.aggregator.infrastructure.marketplace.persistence.entity.CatalogEntryJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CatalogEntryJpaRepository extends JpaRepository<CatalogEntryJpaEntity, UUID> {

    @Query("SELECT e FROM CatalogEntryJpaEntity e WHERE " +
           "(:query IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:entryType IS NULL OR e.entryType = :entryType) " +
           "AND e.status = 'PUBLISHED' " +
           "ORDER BY e.installCount DESC")
    List<CatalogEntryJpaEntity> search(@Param("query") String query,
                                        @Param("entryType") String entryType,
                                        Pageable pageable);

    @Query("SELECT e FROM CatalogEntryJpaEntity e WHERE e.status = 'PUBLISHED' ORDER BY e.installCount DESC")
    List<CatalogEntryJpaEntity> findPopular(Pageable pageable);
}
