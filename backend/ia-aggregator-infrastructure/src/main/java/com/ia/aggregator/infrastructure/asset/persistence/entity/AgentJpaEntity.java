package com.ia.aggregator.infrastructure.asset.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agents", schema = "content")
public class AgentJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "agent_id")
    private String agentId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private String category;

    @Column
    private String icon;

    @Column(name = "default_model")
    private String defaultModel;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "capabilities", columnDefinition = "text[]")
    private String[] capabilities;

    @Column(name = "config", columnDefinition = "jsonb")
    private String config;

    @Column(name = "is_prebuilt", nullable = false)
    private boolean isPrebuilt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AgentJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getCreatedBy() { return createdBy; }
    public String getAgentId() { return agentId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getIcon() { return icon; }
    public String getDefaultModel() { return defaultModel; }
    public String getSystemPrompt() { return systemPrompt; }
    public String[] getCapabilities() { return capabilities; }
    public String getConfig() { return config; }
    public boolean isPrebuilt() { return isPrebuilt; }
    public boolean isActive() { return isActive; }
    public long getUsageCount() { return usageCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public void setCapabilities(String[] capabilities) { this.capabilities = capabilities; }
    public void setConfig(String config) { this.config = config; }
    public void setPrebuilt(boolean isPrebuilt) { this.isPrebuilt = isPrebuilt; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    public void setUsageCount(long usageCount) { this.usageCount = usageCount; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
