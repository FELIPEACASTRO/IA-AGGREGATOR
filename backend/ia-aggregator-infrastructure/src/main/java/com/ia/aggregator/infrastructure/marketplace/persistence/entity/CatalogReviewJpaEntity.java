package com.ia.aggregator.infrastructure.marketplace.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_reviews", schema = "marketplace")
public class CatalogReviewJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CatalogReviewJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getEntryId() { return entryId; }
    public UUID getUserId() { return userId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setEntryId(UUID entryId) { this.entryId = entryId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
