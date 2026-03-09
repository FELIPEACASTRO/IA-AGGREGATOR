package com.ia.aggregator.infrastructure.execution.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tools", schema = "execution")
public class ToolJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "schema_json", columnDefinition = "TEXT")
    private String schemaJson;

    @Column(nullable = false)
    private String scope;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "estimated_cost_usd", nullable = false)
    private BigDecimal estimatedCostUsd;

    @Column
    private String endpoint;

    @Column(columnDefinition = "jsonb")
    private String headers;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ToolJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSchemaJson() { return schemaJson; }
    public String getScope() { return scope; }
    public String getRiskLevel() { return riskLevel; }
    public BigDecimal getEstimatedCostUsd() { return estimatedCostUsd; }
    public String getEndpoint() { return endpoint; }
    public String getHeaders() { return headers; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setSchemaJson(String schemaJson) { this.schemaJson = schemaJson; }
    public void setScope(String scope) { this.scope = scope; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setEstimatedCostUsd(BigDecimal estimatedCostUsd) { this.estimatedCostUsd = estimatedCostUsd; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setHeaders(String headers) { this.headers = headers; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
