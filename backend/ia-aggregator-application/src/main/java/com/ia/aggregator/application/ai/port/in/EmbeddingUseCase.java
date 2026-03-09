package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.EmbeddingRequest;
import com.ia.aggregator.application.ai.dto.EmbeddingResult;

/**
 * Input port for embedding generation.
 * Converts text inputs into dense vector representations.
 */
public interface EmbeddingUseCase {
    EmbeddingResult execute(EmbeddingRequest request);
}
