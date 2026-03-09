package com.ia.aggregator.infrastructure.platform.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "batch_jobs", schema = "platform")
public class BatchJobJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "total_requests", nullable = false)
    private int totalRequests;

    @Column(name = "completed_requests", nullable = false)
    private int completedRequests;

    @Column(name = "failed_requests", nullable = false)
    private int failedRequests;

    @Column(nullable = false)
    private String status;

    @Column(name = "total_cost_usd", nullable = false)
    private BigDecimal totalCostUsd;

    @Column(name = "discount_percent", nullable = false)
    private BigDecimal discountPercent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public BatchJobJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public String getWebhookUrl() { return webhookUrl; }
    public int getTotalRequests() { return totalRequests; }
    public int getCompletedRequests() { return completedRequests; }
    public int getFailedRequests() { return failedRequests; }
    public String getStatus() { return status; }
    public BigDecimal getTotalCostUsd() { return totalCostUsd; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
    public void setCompletedRequests(int completedRequests) { this.completedRequests = completedRequests; }
    public void setFailedRequests(int failedRequests) { this.failedRequests = failedRequests; }
    public void setStatus(String status) { this.status = status; }
    public void setTotalCostUsd(BigDecimal totalCostUsd) { this.totalCostUsd = totalCostUsd; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
