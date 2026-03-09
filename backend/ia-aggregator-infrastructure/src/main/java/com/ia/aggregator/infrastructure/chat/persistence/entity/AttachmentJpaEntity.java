package com.ia.aggregator.infrastructure.chat.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments", schema = "chat")
public class AttachmentJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "attachment_type", nullable = false)
    private String attachmentType;

    @Column(name = "filename")
    private String filename;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_url")
    private String storageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AttachmentJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getMessageId() { return messageId; }
    public String getAttachmentType() { return attachmentType; }
    public String getFilename() { return filename; }
    public String getMimeType() { return mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getStorageUrl() { return storageUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public void setStorageUrl(String storageUrl) { this.storageUrl = storageUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
