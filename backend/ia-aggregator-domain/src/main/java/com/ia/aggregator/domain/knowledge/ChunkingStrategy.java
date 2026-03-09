package com.ia.aggregator.domain.knowledge;

/**
 * Strategies for splitting documents into chunks.
 */
public enum ChunkingStrategy {
    /** Fixed-size chunks with overlap. */
    FIXED_SIZE,
    /** Split by paragraph/section boundaries. */
    SEMANTIC,
    /** Split by sentence boundaries. */
    SENTENCE,
    /** Split by markdown headers. */
    MARKDOWN_HEADER,
    /** No splitting — whole document as single chunk. */
    NONE
}
