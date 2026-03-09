package com.ia.aggregator.application.knowledge.port.out;

import com.ia.aggregator.domain.knowledge.ChunkingStrategy;
import com.ia.aggregator.domain.knowledge.DocumentChunk;

import java.util.List;
import java.util.UUID;

/**
 * Port for document chunking.
 */
public interface DocumentChunkerPort {

    /**
     * Split a document into chunks.
     *
     * @param documentId  Document ID for chunk association
     * @param content     Raw text content
     * @param strategy    Chunking strategy to apply
     * @param chunkSize   Target chunk size in tokens (for FIXED_SIZE)
     * @param overlap     Overlap between chunks in tokens
     * @return ordered list of chunks
     */
    List<DocumentChunk> chunk(UUID documentId, String content, ChunkingStrategy strategy,
                               int chunkSize, int overlap);
}
