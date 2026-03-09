package com.ia.aggregator.infrastructure.chat.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations", schema = "chat")
public class ConversationJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title")
    private String title;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "model")
    private String model;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "total_tokens", nullable = false)
    private long totalTokens = 0;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public ConversationJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getModel() { return model; }
    public boolean isPinned() { return pinned; }
    public int getMessageCount() { return messageCount; }
    public long getTotalTokens() { return totalTokens; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setStatus(String status) { this.status = status; }
    public void setModel(String model) { this.model = model; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
    public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
