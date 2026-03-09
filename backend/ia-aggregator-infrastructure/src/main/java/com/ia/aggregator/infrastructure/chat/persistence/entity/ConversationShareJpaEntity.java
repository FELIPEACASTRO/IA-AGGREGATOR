package com.ia.aggregator.infrastructure.chat.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_shares", schema = "chat")
public class ConversationShareJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "shared_by", nullable = false)
    private UUID sharedBy;

    @Column(name = "share_token", nullable = false, unique = true)
    private String shareToken;

    @Column(name = "visibility", nullable = false)
    private String visibility;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ConversationShareJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public UUID getSharedBy() { return sharedBy; }
    public String getShareToken() { return shareToken; }
    public String getVisibility() { return visibility; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getViewCount() { return viewCount; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public void setSharedBy(UUID sharedBy) { this.sharedBy = sharedBy; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
