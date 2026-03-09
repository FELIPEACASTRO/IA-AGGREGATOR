package com.ia.aggregator.infrastructure.platform.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "virtual_keys", schema = "platform")
public class VirtualKeyJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private String name;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix;

    @Column(name = "allowed_models", columnDefinition = "text[]")
    private String[] allowedModels;

    @Column(name = "allowed_capabilities", columnDefinition = "text[]")
    private String[] allowedCapabilities;

    @Column(name = "rate_limit_rpm", nullable = false)
    private int rateLimitRpm;

    @Column(name = "budget_limit_usd", nullable = false)
    private BigDecimal budgetLimitUsd;

    @Column(name = "spent_usd", nullable = false)
    private BigDecimal spentUsd;

    @Column(nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public VirtualKeyJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getCreatedBy() { return createdBy; }
    public String getName() { return name; }
    public String getKeyHash() { return keyHash; }
    public String getKeyPrefix() { return keyPrefix; }
    public String[] getAllowedModels() { return allowedModels; }
    public String[] getAllowedCapabilities() { return allowedCapabilities; }
    public int getRateLimitRpm() { return rateLimitRpm; }
    public BigDecimal getBudgetLimitUsd() { return budgetLimitUsd; }
    public BigDecimal getSpentUsd() { return spentUsd; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getExpiresAt() { return expiresAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setName(String name) { this.name = name; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public void setAllowedModels(String[] allowedModels) { this.allowedModels = allowedModels; }
    public void setAllowedCapabilities(String[] allowedCapabilities) { this.allowedCapabilities = allowedCapabilities; }
    public void setRateLimitRpm(int rateLimitRpm) { this.rateLimitRpm = rateLimitRpm; }
    public void setBudgetLimitUsd(BigDecimal budgetLimitUsd) { this.budgetLimitUsd = budgetLimitUsd; }
    public void setSpentUsd(BigDecimal spentUsd) { this.spentUsd = spentUsd; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
