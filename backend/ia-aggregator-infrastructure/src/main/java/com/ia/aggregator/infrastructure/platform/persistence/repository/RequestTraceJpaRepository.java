package com.ia.aggregator.infrastructure.platform.persistence.repository;

import com.ia.aggregator.infrastructure.platform.persistence.entity.RequestTraceJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RequestTraceJpaRepository extends JpaRepository<RequestTraceJpaEntity, UUID> {

    List<RequestTraceJpaEntity> findByOrgIdAndTimestampBetween(UUID orgId, Instant from, Instant to, Pageable pageable);

    List<RequestTraceJpaEntity> findByVirtualKeyIdAndTimestampBetween(UUID virtualKeyId, Instant from, Instant to, Pageable pageable);

    long countByOrgIdAndTimestampBetween(UUID orgId, Instant from, Instant to);

    @Query("SELECT COUNT(r) FROM RequestTraceJpaEntity r WHERE r.orgId = :orgId AND r.timestamp BETWEEN :from AND :to AND r.statusCode >= 200 AND r.statusCode < 300")
    long countSuccessByOrgIdAndTimestampBetween(@Param("orgId") UUID orgId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(r.costUsd), 0) FROM RequestTraceJpaEntity r WHERE r.orgId = :orgId AND r.timestamp BETWEEN :from AND :to")
    double sumCostByOrgIdAndTimestampBetween(@Param("orgId") UUID orgId, @Param("from") Instant from, @Param("to") Instant to);
}
