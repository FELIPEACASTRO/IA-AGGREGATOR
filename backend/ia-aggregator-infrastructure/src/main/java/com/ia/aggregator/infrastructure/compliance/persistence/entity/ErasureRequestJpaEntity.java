package com.ia.aggregator.infrastructure.compliance.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "erasure_requests", schema = "auth")
public class ErasureRequestJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String status;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "erased_tables", columnDefinition = "text[]")
    private String[] erasedTables;

    @Column(name = "data_retained", columnDefinition = "text[]")
    private String[] dataRetained;

    @Column(name = "retention_reason")
    private String retentionReason;

    @Column
    private String notes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ErasureRequestJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public UUID getProcessedBy() { return processedBy; }
    public String[] getErasedTables() { return erasedTables; }
    public String[] getDataRetained() { return dataRetained; }
    public String getRetentionReason() { return retentionReason; }
    public String getNotes() { return notes; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setStatus(String status) { this.status = status; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public void setProcessedBy(UUID processedBy) { this.processedBy = processedBy; }
    public void setErasedTables(String[] erasedTables) { this.erasedTables = erasedTables; }
    public void setDataRetained(String[] dataRetained) { this.dataRetained = dataRetained; }
    public void setRetentionReason(String retentionReason) { this.retentionReason = retentionReason; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
