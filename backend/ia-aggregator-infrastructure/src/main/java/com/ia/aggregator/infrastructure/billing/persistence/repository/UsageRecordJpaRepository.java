package com.ia.aggregator.infrastructure.billing.persistence.repository;

import com.ia.aggregator.infrastructure.billing.persistence.entity.UsageRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageRecordJpaRepository extends JpaRepository<UsageRecordJpaEntity, UUID> {

    List<UsageRecordJpaEntity> findByOrgIdAndPeriod(UUID orgId, String period);

    List<UsageRecordJpaEntity> findByOrgIdAndRecordedAtAfter(UUID orgId, Instant since);

    List<UsageRecordJpaEntity> findByOrgIdAndRecordedAtBetween(UUID orgId, Instant from, Instant to);

    @Query("SELECT COALESCE(SUM(u.amount), 0) FROM UsageRecordJpaEntity u WHERE u.orgId = :orgId AND u.recordedAt BETWEEN :from AND :to")
    long sumAmountByOrgIdAndRecordedAtBetween(@Param("orgId") UUID orgId, @Param("from") Instant from, @Param("to") Instant to);

    long countByOrgIdAndRecordedAtBetween(UUID orgId, Instant from, Instant to);
}
