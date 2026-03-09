package com.ia.aggregator.application.knowledge.port.out;

import java.util.List;

/**
 * Port for generating text embeddings.
 */
public interface EmbeddingPort {

    /**
     * Generate embeddings for a list of texts.
     *
     * @param texts list of texts to embed
     * @param model embedding model to use
     * @return list of embedding vectors (parallel with input)
     */
    List<float[]> embed(List<String> texts, String model);

    /**
     * Generate a single embedding for a query.
     */
    float[] embedQuery(String text, String model);

    /**
     * Get the dimensionality of embeddings for a model.
     */
    int getDimensions(String model);
}
