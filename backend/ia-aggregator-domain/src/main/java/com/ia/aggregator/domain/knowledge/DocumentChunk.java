package com.ia.aggregator.domain.knowledge;

import java.util.Map;
import java.util.UUID;

/**
 * A chunk of a document with its embedding vector.
 *
 * @param id          Unique chunk identifier
 * @param documentId  Parent document ID
 * @param index       Chunk position within the document
 * @param content     Text content of the chunk
 * @param tokenCount  Approximate token count
 * @param embedding   Vector embedding (nullable until embedded)
 * @param metadata    Chunk-level metadata (page number, section title, etc.)
 */
public record DocumentChunk(
        UUID id,
        UUID documentId,
        int index,
        String content,
        int tokenCount,
        float[] embedding,
        Map<String, String> metadata
) {
    public DocumentChunk {
        if (id == null) id = UUID.randomUUID();
        if (metadata == null) metadata = Map.of();
    }
}
