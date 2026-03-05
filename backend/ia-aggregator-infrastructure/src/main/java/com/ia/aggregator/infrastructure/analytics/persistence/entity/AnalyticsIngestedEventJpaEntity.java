package com.ia.aggregator.infrastructure.analytics.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingested_events", schema = "analytics")
public class AnalyticsIngestedEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "report_id", nullable = false, columnDefinition = "uuid")
    private UUID reportId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_category", nullable = false)
    private String eventCategory;

    @Column(name = "event_timestamp")
    private Instant eventTimestamp;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public UUID getReportId() {
        return reportId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
