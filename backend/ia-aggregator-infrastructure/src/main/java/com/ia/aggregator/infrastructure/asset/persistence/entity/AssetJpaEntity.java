package com.ia.aggregator.infrastructure.asset.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assets", schema = "content")
public class AssetJpaEntity {

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

    @Column(name = "asset_type", nullable = false)
    private String assetType;

    @Column(nullable = false)
    private String scope;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "jsonb")
    private String variables;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    @Column(name = "average_rating", nullable = false)
    private double averageRating;

    @Column(name = "total_cost_usd", nullable = false)
    private double totalCostUsd;

    @Column(name = "success_count", nullable = false)
    private long successCount;

    @Column(name = "failure_count", nullable = false)
    private long failureCount;

    @Column(nullable = false)
    private boolean published;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AssetJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getCreatedBy() { return createdBy; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAssetType() { return assetType; }
    public String getScope() { return scope; }
    public String getContent() { return content; }
    public String getVariables() { return variables; }
    public String getMetadata() { return metadata; }
    public long getUsageCount() { return usageCount; }
    public double getAverageRating() { return averageRating; }
    public double getTotalCostUsd() { return totalCostUsd; }
    public long getSuccessCount() { return successCount; }
    public long getFailureCount() { return failureCount; }
    public boolean isPublished() { return published; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public void setScope(String scope) { this.scope = scope; }
    public void setContent(String content) { this.content = content; }
    public void setVariables(String variables) { this.variables = variables; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setUsageCount(long usageCount) { this.usageCount = usageCount; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setTotalCostUsd(double totalCostUsd) { this.totalCostUsd = totalCostUsd; }
    public void setSuccessCount(long successCount) { this.successCount = successCount; }
    public void setFailureCount(long failureCount) { this.failureCount = failureCount; }
    public void setPublished(boolean published) { this.published = published; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
