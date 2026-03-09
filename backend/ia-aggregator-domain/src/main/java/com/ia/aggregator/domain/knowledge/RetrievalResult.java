package com.ia.aggregator.domain.knowledge;

import java.util.UUID;

/**
 * A single retrieval result from vector search.
 *
 * @param chunkId      Chunk ID
 * @param documentId   Parent document ID
 * @param documentTitle Document title
 * @param content      Chunk content
 * @param score        Similarity score (0-1)
 * @param pageNumber   Source page number if available
 */
public record RetrievalResult(
        UUID chunkId,
        UUID documentId,
        String documentTitle,
        String content,
        double score,
        Integer pageNumber
) {}
