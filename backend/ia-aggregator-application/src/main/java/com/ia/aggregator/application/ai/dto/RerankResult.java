package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Result DTO for document re-ranking.
 *
 * @param results ranked documents with scores, ordered by relevance (descending)
 * @param modelUsed actual model used
 * @param providerUsed provider that performed the reranking
 */
public record RerankResult(
        List<ScoredDocument> results,
        String modelUsed,
        String providerUsed
) {
    /**
     * A document with its relevance score.
     *
     * @param index original document index
     * @param score relevance score [0.0, 1.0]
     * @param document the original document
     */
    public record ScoredDocument(int index, double score, RerankDocument document) {
    }
}
