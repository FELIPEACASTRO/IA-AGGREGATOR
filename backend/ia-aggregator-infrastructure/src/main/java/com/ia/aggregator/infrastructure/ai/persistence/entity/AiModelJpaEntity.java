package com.ia.aggregator.infrastructure.ai.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "models", schema = "ai_gateway")
public class AiModelJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private String category;

    @Column(name = "context_window")
    private Integer contextWindow;

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;

    @Column(name = "input_cost_per_1k")
    private Double inputCostPer1k;

    @Column(name = "output_cost_per_1k")
    private Double outputCostPer1k;

    @Column(name = "supports_streaming", nullable = false)
    private boolean supportsStreaming;

    @Column(name = "supports_function_calling", nullable = false)
    private boolean supportsFunctionCalling;

    @Column(name = "supports_vision", nullable = false)
    private boolean supportsVision;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column
    private String tier;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AiModelJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getProviderId() { return providerId; }
    public String getModelId() { return modelId; }
    public String getDisplayName() { return displayName; }
    public String getCategory() { return category; }
    public Integer getContextWindow() { return contextWindow; }
    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public Double getInputCostPer1k() { return inputCostPer1k; }
    public Double getOutputCostPer1k() { return outputCostPer1k; }
    public boolean isSupportsStreaming() { return supportsStreaming; }
    public boolean isSupportsFunctionCalling() { return supportsFunctionCalling; }
    public boolean isSupportsVision() { return supportsVision; }
    public boolean isDefault() { return isDefault; }
    public boolean isActive() { return isActive; }
    public String getTier() { return tier; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setCategory(String category) { this.category = category; }
    public void setContextWindow(Integer contextWindow) { this.contextWindow = contextWindow; }
    public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public void setInputCostPer1k(Double inputCostPer1k) { this.inputCostPer1k = inputCostPer1k; }
    public void setOutputCostPer1k(Double outputCostPer1k) { this.outputCostPer1k = outputCostPer1k; }
    public void setSupportsStreaming(boolean supportsStreaming) { this.supportsStreaming = supportsStreaming; }
    public void setSupportsFunctionCalling(boolean supportsFunctionCalling) { this.supportsFunctionCalling = supportsFunctionCalling; }
    public void setSupportsVision(boolean supportsVision) { this.supportsVision = supportsVision; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
    public void setTier(String tier) { this.tier = tier; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
