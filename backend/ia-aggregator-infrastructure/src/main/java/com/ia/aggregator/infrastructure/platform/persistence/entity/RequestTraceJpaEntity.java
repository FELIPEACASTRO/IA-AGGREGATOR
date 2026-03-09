package com.ia.aggregator.infrastructure.platform.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_traces", schema = "platform")
public class RequestTraceJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "virtual_key_id")
    private UUID virtualKeyId;

    @Column(nullable = false)
    private String capability;

    @Column(name = "requested_model")
    private String requestedModel;

    @Column(name = "used_model")
    private String usedModel;

    @Column
    private String provider;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    @Column(name = "cost_usd", nullable = false)
    private BigDecimal costUsd;

    @Column(name = "total_latency_ms", nullable = false)
    private long totalLatencyMs;

    @Column(name = "provider_latency_ms", nullable = false)
    private long providerLatencyMs;

    @Column(name = "routing_latency_ms", nullable = false)
    private long routingLatencyMs;

    @Column(name = "cache_hit", nullable = false)
    private boolean cacheHit;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(nullable = false)
    private Instant timestamp;

    public RequestTraceJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getVirtualKeyId() { return virtualKeyId; }
    public String getCapability() { return capability; }
    public String getRequestedModel() { return requestedModel; }
    public String getUsedModel() { return usedModel; }
    public String getProvider() { return provider; }
    public int getStatusCode() { return statusCode; }
    public long getInputTokens() { return inputTokens; }
    public long getOutputTokens() { return outputTokens; }
    public BigDecimal getCostUsd() { return costUsd; }
    public long getTotalLatencyMs() { return totalLatencyMs; }
    public long getProviderLatencyMs() { return providerLatencyMs; }
    public long getRoutingLatencyMs() { return routingLatencyMs; }
    public boolean isCacheHit() { return cacheHit; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getTimestamp() { return timestamp; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setVirtualKeyId(UUID virtualKeyId) { this.virtualKeyId = virtualKeyId; }
    public void setCapability(String capability) { this.capability = capability; }
    public void setRequestedModel(String requestedModel) { this.requestedModel = requestedModel; }
    public void setUsedModel(String usedModel) { this.usedModel = usedModel; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public void setInputTokens(long inputTokens) { this.inputTokens = inputTokens; }
    public void setOutputTokens(long outputTokens) { this.outputTokens = outputTokens; }
    public void setCostUsd(BigDecimal costUsd) { this.costUsd = costUsd; }
    public void setTotalLatencyMs(long totalLatencyMs) { this.totalLatencyMs = totalLatencyMs; }
    public void setProviderLatencyMs(long providerLatencyMs) { this.providerLatencyMs = providerLatencyMs; }
    public void setRoutingLatencyMs(long routingLatencyMs) { this.routingLatencyMs = routingLatencyMs; }
    public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
    public void setFallbackUsed(boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
