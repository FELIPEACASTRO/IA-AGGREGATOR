package com.ia.aggregator.infrastructure.compliance.persistence.repository;

import com.ia.aggregator.infrastructure.compliance.persistence.entity.ErasureRequestJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ErasureRequestJpaRepository extends JpaRepository<ErasureRequestJpaEntity, UUID> {

    @Query("SELECT e FROM ErasureRequestJpaEntity e WHERE e.userId IN " +
           "(SELECT u.id FROM UserJpaEntity u WHERE u.personalOrgId = :orgId) " +
           "ORDER BY e.createdAt DESC")
    List<ErasureRequestJpaEntity> findByOrgId(@Param("orgId") UUID orgId, Pageable pageable);
}
