package com.ia.aggregator.domain.knowledge;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A document in the knowledge base.
 *
 * @param id              Unique document identifier
 * @param orgId           Organization that owns this document
 * @param collectionId    Knowledge base collection ID
 * @param title           Document title
 * @param sourceUrl       Original source URL (nullable)
 * @param mimeType        MIME type (application/pdf, text/plain, etc.)
 * @param sizeBytes       Document size in bytes
 * @param status          Lifecycle status
 * @param chunkCount      Number of chunks after processing
 * @param chunkingStrategy Strategy used for chunking
 * @param embeddingModel  Model used to generate embeddings
 * @param metadata        Additional metadata (author, tags, etc.)
 * @param createdAt       When the document was uploaded
 * @param updatedAt       Last update timestamp
 * @param errorMessage    Error message if status is FAILED
 */
public record KnowledgeDocument(
        UUID id,
        UUID orgId,
        UUID collectionId,
        String title,
        String sourceUrl,
        String mimeType,
        long sizeBytes,
        DocumentStatus status,
        int chunkCount,
        ChunkingStrategy chunkingStrategy,
        String embeddingModel,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt,
        String errorMessage
) {
    public KnowledgeDocument {
        if (id == null) id = UUID.randomUUID();
        if (metadata == null) metadata = Map.of();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    public static KnowledgeDocument create(UUID orgId, UUID collectionId, String title,
                                            String mimeType, long sizeBytes,
                                            ChunkingStrategy strategy) {
        return new KnowledgeDocument(UUID.randomUUID(), orgId, collectionId, title,
                null, mimeType, sizeBytes, DocumentStatus.PENDING, 0,
                strategy, null, Map.of(), Instant.now(), Instant.now(), null);
    }
}
