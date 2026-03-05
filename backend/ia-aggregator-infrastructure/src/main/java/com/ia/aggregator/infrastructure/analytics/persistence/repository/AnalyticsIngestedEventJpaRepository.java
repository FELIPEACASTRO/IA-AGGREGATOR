package com.ia.aggregator.infrastructure.analytics.persistence.repository;

import com.ia.aggregator.infrastructure.analytics.persistence.entity.AnalyticsIngestedEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsIngestedEventJpaRepository extends JpaRepository<AnalyticsIngestedEventJpaEntity, UUID> {
    @Query(value = """
	    SELECT id, report_id, event_name, event_category, event_timestamp, metadata, created_at
	    FROM analytics.ingested_events
	    WHERE report_id = :reportId
	      AND (:category IS NULL OR event_category = :category)
	    ORDER BY event_timestamp DESC NULLS LAST, created_at DESC
	    LIMIT :limit OFFSET :offset
	    """, nativeQuery = true)
    List<AnalyticsIngestedEventJpaEntity> findReportEvents(
	    @Param("reportId") UUID reportId,
	    @Param("category") String category,
	    @Param("limit") int limit,
	    @Param("offset") int offset
    );
}
