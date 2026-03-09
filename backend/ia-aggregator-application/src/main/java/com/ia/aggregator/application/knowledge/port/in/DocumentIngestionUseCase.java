package com.ia.aggregator.application.knowledge.port.in;

import com.ia.aggregator.domain.knowledge.ChunkingStrategy;
import com.ia.aggregator.domain.knowledge.KnowledgeDocument;

import java.util.UUID;

/**
 * Use case for ingesting documents into the knowledge base.
 */
public interface DocumentIngestionUseCase {

    /**
     * Ingest a document: parse, chunk, embed, and store.
     *
     * @param orgId         Organization ID
     * @param collectionId  Knowledge base collection
     * @param title         Document title
     * @param content       Raw document content (text or bytes as base64)
     * @param mimeType      MIME type
     * @param strategy      Chunking strategy
     * @return the created document with status
     */
    KnowledgeDocument ingest(UUID orgId, UUID collectionId, String title,
                              String content, String mimeType, ChunkingStrategy strategy);

    /**
     * Get the status of a document ingestion.
     */
    KnowledgeDocument getStatus(UUID documentId);

    /**
     * Delete a document and its chunks from the knowledge base.
     */
    void delete(UUID documentId);
}
