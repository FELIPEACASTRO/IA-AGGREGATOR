package com.ia.aggregator.infrastructure.marketplace.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "catalog_entries", schema = "marketplace")
public class CatalogEntryJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "publisher_id", nullable = false)
    private UUID publisherId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(name = "categories", columnDefinition = "text[]")
    private String[] categories;

    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column
    private String version;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "revenue_share_percent", nullable = false)
    private BigDecimal revenueSharePercent;

    @Column(name = "average_rating", nullable = false)
    private BigDecimal averageRating;

    @Column(name = "install_count", nullable = false)
    private long installCount;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "published_at")
    private Instant publishedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CatalogEntryJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getPublisherId() { return publisherId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getEntryType() { return entryType; }
    public String[] getCategories() { return categories; }
    public String[] getTags() { return tags; }
    public String getVersion() { return version; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getRevenueSharePercent() { return revenueSharePercent; }
    public BigDecimal getAverageRating() { return averageRating; }
    public long getInstallCount() { return installCount; }
    public String getStatus() { return status; }
    public String getMetadata() { return metadata; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setPublisherId(UUID publisherId) { this.publisherId = publisherId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public void setCategories(String[] categories) { this.categories = categories; }
    public void setTags(String[] tags) { this.tags = tags; }
    public void setVersion(String version) { this.version = version; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setRevenueSharePercent(BigDecimal revenueSharePercent) { this.revenueSharePercent = revenueSharePercent; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }
    public void setInstallCount(long installCount) { this.installCount = installCount; }
    public void setStatus(String status) { this.status = status; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
