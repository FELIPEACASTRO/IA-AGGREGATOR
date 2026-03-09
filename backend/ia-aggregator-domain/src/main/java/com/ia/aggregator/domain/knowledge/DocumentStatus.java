package com.ia.aggregator.domain.knowledge;

/**
 * Lifecycle states for a knowledge base document.
 */
public enum DocumentStatus {
    PENDING,
    PROCESSING,
    CHUNKING,
    EMBEDDING,
    INDEXED,
    FAILED,
    DELETED
}
