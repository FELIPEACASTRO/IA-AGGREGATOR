package com.ia.aggregator.infrastructure.billing.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_records", schema = "billing")
public class UsageRecordJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(nullable = false)
    private String metric;

    @Column(nullable = false)
    private long amount;

    @Column
    private String unit;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "provider_used")
    private String providerUsed;

    @Column(nullable = false)
    private String period;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UsageRecordJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getUserId() { return userId; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public String getMetric() { return metric; }
    public long getAmount() { return amount; }
    public String getUnit() { return unit; }
    public String getModelUsed() { return modelUsed; }
    public String getProviderUsed() { return providerUsed; }
    public String getPeriod() { return period; }
    public String getMetadata() { return metadata; }
    public Instant getRecordedAt() { return recordedAt; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setSubscriptionId(UUID subscriptionId) { this.subscriptionId = subscriptionId; }
    public void setMetric(String metric) { this.metric = metric; }
    public void setAmount(long amount) { this.amount = amount; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    public void setProviderUsed(String providerUsed) { this.providerUsed = providerUsed; }
    public void setPeriod(String period) { this.period = period; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
