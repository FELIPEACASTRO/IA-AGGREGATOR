package com.ia.aggregator.infrastructure.ai.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_metrics", schema = "ai_gateway")
public class ProviderMetricJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_id")
    private String modelId;

    @Column(nullable = false)
    private String period;

    @Column(name = "request_count", nullable = false)
    private long requestCount;

    @Column(name = "success_count", nullable = false)
    private long successCount;

    @Column(name = "error_count", nullable = false)
    private long errorCount;

    @Column(name = "total_tokens", nullable = false)
    private long totalTokens;

    @Column(name = "total_cost_usd", nullable = false)
    private double totalCostUsd;

    @Column(name = "avg_latency_ms", nullable = false)
    private int avgLatencyMs;

    @Column(name = "p50_latency_ms", nullable = false)
    private int p50LatencyMs;

    @Column(name = "p95_latency_ms", nullable = false)
    private int p95LatencyMs;

    @Column(name = "p99_latency_ms", nullable = false)
    private int p99LatencyMs;

    @Column(name = "quality_score", nullable = false)
    private double qualityScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ProviderMetricJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getProviderId() { return providerId; }
    public String getModelId() { return modelId; }
    public String getPeriod() { return period; }
    public long getRequestCount() { return requestCount; }
    public long getSuccessCount() { return successCount; }
    public long getErrorCount() { return errorCount; }
    public long getTotalTokens() { return totalTokens; }
    public double getTotalCostUsd() { return totalCostUsd; }
    public int getAvgLatencyMs() { return avgLatencyMs; }
    public int getP50LatencyMs() { return p50LatencyMs; }
    public int getP95LatencyMs() { return p95LatencyMs; }
    public int getP99LatencyMs() { return p99LatencyMs; }
    public double getQualityScore() { return qualityScore; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public void setPeriod(String period) { this.period = period; }
    public void setRequestCount(long requestCount) { this.requestCount = requestCount; }
    public void setSuccessCount(long successCount) { this.successCount = successCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
    public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
    public void setTotalCostUsd(double totalCostUsd) { this.totalCostUsd = totalCostUsd; }
    public void setAvgLatencyMs(int avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }
    public void setP50LatencyMs(int p50LatencyMs) { this.p50LatencyMs = p50LatencyMs; }
    public void setP95LatencyMs(int p95LatencyMs) { this.p95LatencyMs = p95LatencyMs; }
    public void setP99LatencyMs(int p99LatencyMs) { this.p99LatencyMs = p99LatencyMs; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
