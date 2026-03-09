package com.ia.aggregator.infrastructure.billing.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budgets", schema = "billing")
public class BudgetJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String name;

    @Column(name = "monthly_limit_cents", nullable = false)
    private int monthlyLimitCents;

    @Column(name = "alert_threshold_pct", nullable = false)
    private int alertThresholdPct;

    @Column(nullable = false)
    private String action;

    @Column(name = "current_spend_cents", nullable = false)
    private int currentSpendCents;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BudgetJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public String getName() { return name; }
    public int getMonthlyLimitCents() { return monthlyLimitCents; }
    public int getAlertThresholdPct() { return alertThresholdPct; }
    public String getAction() { return action; }
    public int getCurrentSpendCents() { return currentSpendCents; }
    public boolean isActive() { return isActive; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setName(String name) { this.name = name; }
    public void setMonthlyLimitCents(int monthlyLimitCents) { this.monthlyLimitCents = monthlyLimitCents; }
    public void setAlertThresholdPct(int alertThresholdPct) { this.alertThresholdPct = alertThresholdPct; }
    public void setAction(String action) { this.action = action; }
    public void setCurrentSpendCents(int currentSpendCents) { this.currentSpendCents = currentSpendCents; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
