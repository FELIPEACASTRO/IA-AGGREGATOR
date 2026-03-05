package com.ia.aggregator.infrastructure.analytics.persistence.repository;

import com.ia.aggregator.infrastructure.analytics.persistence.entity.AnalyticsEventReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalyticsEventReportJpaRepository
        extends JpaRepository<AnalyticsEventReportJpaEntity, UUID>, JpaSpecificationExecutor<AnalyticsEventReportJpaEntity> {
}
