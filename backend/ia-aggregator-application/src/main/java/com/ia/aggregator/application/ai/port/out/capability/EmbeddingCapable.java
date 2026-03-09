package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.EmbeddingRequest;
import com.ia.aggregator.application.ai.dto.EmbeddingResult;

/**
 * Capability interface for text-to-vector embedding generation.
 * Providers implementing this can convert text into dense vector representations.
 */
public interface EmbeddingCapable {

    /**
     * Generates embeddings for the given input texts.
     *
     * @param request the embedding request containing input texts and model
     * @return the embedding result with vectors and usage metadata
     */
    EmbeddingResult embed(EmbeddingRequest request);
}
