package com.ia.aggregator.infrastructure.billing.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plans", schema = "billing")
public class BillingPlanJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tier;

    @Column(name = "price_display")
    private String priceDisplay;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Column(nullable = false)
    private String currency;

    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle;

    @Column
    private String description;

    @Column(name = "token_limit", nullable = false)
    private long tokenLimit;

    @Column(name = "model_count", nullable = false)
    private int modelCount;

    @Column(name = "features", columnDefinition = "jsonb")
    private String features;

    @Column
    private String gradient;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BillingPlanJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getTier() { return tier; }
    public String getPriceDisplay() { return priceDisplay; }
    public int getPriceCents() { return priceCents; }
    public String getCurrency() { return currency; }
    public String getBillingCycle() { return billingCycle; }
    public String getDescription() { return description; }
    public long getTokenLimit() { return tokenLimit; }
    public int getModelCount() { return modelCount; }
    public String getFeatures() { return features; }
    public String getGradient() { return gradient; }
    public boolean isActive() { return isActive; }
    public int getSortOrder() { return sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setName(String name) { this.name = name; }
    public void setTier(String tier) { this.tier = tier; }
    public void setPriceDisplay(String priceDisplay) { this.priceDisplay = priceDisplay; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public void setDescription(String description) { this.description = description; }
    public void setTokenLimit(long tokenLimit) { this.tokenLimit = tokenLimit; }
    public void setModelCount(int modelCount) { this.modelCount = modelCount; }
    public void setFeatures(String features) { this.features = features; }
    public void setGradient(String gradient) { this.gradient = gradient; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
