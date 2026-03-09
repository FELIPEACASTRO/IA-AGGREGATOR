package com.ia.aggregator.infrastructure.knowledge.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_documents", schema = "content")
public class KnowledgeDocumentJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "collection_id", nullable = false)
    private UUID collectionId;

    @Column(nullable = false)
    private String title;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String status;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "chunking_strategy", nullable = false)
    private String chunkingStrategy;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public KnowledgeDocumentJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getOrgId() { return orgId; }
    public UUID getCollectionId() { return collectionId; }
    public String getTitle() { return title; }
    public String getSourceUrl() { return sourceUrl; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStatus() { return status; }
    public int getChunkCount() { return chunkCount; }
    public String getChunkingStrategy() { return chunkingStrategy; }
    public String getEmbeddingModel() { return embeddingModel; }
    public String getErrorMessage() { return errorMessage; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setCollectionId(UUID collectionId) { this.collectionId = collectionId; }
    public void setTitle(String title) { this.title = title; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public void setStatus(String status) { this.status = status; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public void setChunkingStrategy(String chunkingStrategy) { this.chunkingStrategy = chunkingStrategy; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
