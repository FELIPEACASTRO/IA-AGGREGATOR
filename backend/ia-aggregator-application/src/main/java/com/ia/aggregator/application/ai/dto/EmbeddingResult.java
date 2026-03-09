package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Result DTO for embedding generation.
 *
 * @param embeddings list of embedding vectors (one per input text)
 * @param modelUsed actual model used
 * @param providerUsed provider that generated the embeddings
 * @param dimensions dimensionality of each embedding vector
 * @param totalTokens total tokens consumed
 */
public record EmbeddingResult(
        List<float[]> embeddings,
        String modelUsed,
        String providerUsed,
        int dimensions,
        Integer totalTokens
) {
}
