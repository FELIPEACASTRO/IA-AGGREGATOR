package com.ia.aggregator.infrastructure.execution.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflows", schema = "execution")
public class WorkflowJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(columnDefinition = "jsonb")
    private String steps;

    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private String triggerConfig;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WorkflowJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getCreatedBy() { return createdBy; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSteps() { return steps; }
    public String getTriggerConfig() { return triggerConfig; }
    public String getStatus() { return status; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setSteps(String steps) { this.steps = steps; }
    public void setTriggerConfig(String triggerConfig) { this.triggerConfig = triggerConfig; }
    public void setStatus(String status) { this.status = status; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
