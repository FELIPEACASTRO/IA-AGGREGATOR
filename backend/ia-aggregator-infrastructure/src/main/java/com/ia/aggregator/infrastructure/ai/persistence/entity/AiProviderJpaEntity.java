package com.ia.aggregator.infrastructure.ai.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "providers", schema = "ai_gateway")
public class AiProviderJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private String status;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(columnDefinition = "text")
    private String capabilities;

    @Column(name = "supported_models", columnDefinition = "text")
    private String supportedModels;

    @Column(name = "auth_type")
    private String authType;

    @Column(name = "rate_limit_rpm")
    private Integer rateLimitRpm;

    @Column(name = "rate_limit_tpm")
    private Long rateLimitTpm;

    @Column(columnDefinition = "jsonb")
    private String config;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "last_health_check")
    private Instant lastHealthCheck;

    @Column(name = "last_health_status")
    private String lastHealthStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AiProviderJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getStatus() { return status; }
    public String getBaseUrl() { return baseUrl; }
    public String getCapabilities() { return capabilities; }
    public String getSupportedModels() { return supportedModels; }
    public String getAuthType() { return authType; }
    public Integer getRateLimitRpm() { return rateLimitRpm; }
    public Long getRateLimitTpm() { return rateLimitTpm; }
    public String getConfig() { return config; }
    public String getMetadata() { return metadata; }
    public Instant getLastHealthCheck() { return lastHealthCheck; }
    public String getLastHealthStatus() { return lastHealthStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setStatus(String status) { this.status = status; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }
    public void setSupportedModels(String supportedModels) { this.supportedModels = supportedModels; }
    public void setAuthType(String authType) { this.authType = authType; }
    public void setRateLimitRpm(Integer rateLimitRpm) { this.rateLimitRpm = rateLimitRpm; }
    public void setRateLimitTpm(Long rateLimitTpm) { this.rateLimitTpm = rateLimitTpm; }
    public void setConfig(String config) { this.config = config; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setLastHealthCheck(Instant lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; }
    public void setLastHealthStatus(String lastHealthStatus) { this.lastHealthStatus = lastHealthStatus; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
