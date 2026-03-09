package com.ia.aggregator.application.knowledge.port.out;

import com.ia.aggregator.domain.knowledge.DocumentChunk;
import com.ia.aggregator.domain.knowledge.RetrievalResult;

import java.util.List;
import java.util.UUID;

/**
 * Port for vector database operations (Qdrant, pgvector, etc.).
 */
public interface VectorStorePort {

    /**
     * Upsert chunks with embeddings into the vector store.
     */
    void upsertChunks(UUID collectionId, List<DocumentChunk> chunks);

    /**
     * Search for similar chunks by query embedding.
     *
     * @param collectionId   Collection to search
     * @param queryEmbedding Query vector
     * @param topK           Number of results
     * @param scoreThreshold Minimum similarity score
     * @return ranked results
     */
    List<RetrievalResult> search(UUID collectionId, float[] queryEmbedding, int topK, double scoreThreshold);

    /**
     * Delete all chunks for a document.
     */
    void deleteByDocument(UUID documentId);

    /**
     * Create a new collection/namespace.
     */
    void createCollection(UUID collectionId, int dimensions);

    /**
     * Delete a collection and all its data.
     */
    void deleteCollection(UUID collectionId);
}
