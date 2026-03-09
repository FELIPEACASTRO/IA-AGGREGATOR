package com.ia.aggregator.application.knowledge.port.out;

import com.ia.aggregator.domain.knowledge.KnowledgeDocument;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for knowledge document persistence.
 */
public interface KnowledgeDocumentRepository {

    void save(KnowledgeDocument document);

    Optional<KnowledgeDocument> findById(UUID documentId);

    void delete(UUID documentId);
}
