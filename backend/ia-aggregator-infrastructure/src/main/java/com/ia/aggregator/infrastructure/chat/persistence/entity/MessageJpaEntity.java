package com.ia.aggregator.infrastructure.chat.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages", schema = "chat")
public class MessageJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "provider_used")
    private String providerUsed;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed = false;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 12, scale = 8)
    private java.math.BigDecimal costUsd;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MessageJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public String getModelUsed() { return modelUsed; }
    public String getProviderUsed() { return providerUsed; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public int getAttempts() { return attempts; }
    public Integer getInputTokens() { return inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public java.math.BigDecimal getCostUsd() { return costUsd; }
    public Integer getLatencyMs() { return latencyMs; }
    public UUID getParentId() { return parentId; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setStatus(String status) { this.status = status; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    public void setProviderUsed(String providerUsed) { this.providerUsed = providerUsed; }
    public void setFallbackUsed(boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    public void setCostUsd(java.math.BigDecimal costUsd) { this.costUsd = costUsd; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
