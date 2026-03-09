package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for text embedding generation.
 *
 * @param input list of texts to embed (required, non-empty)
 * @param model preferred embedding model (optional)
 * @param dimensions desired embedding dimensions (optional — provider default if null)
 */
public record EmbeddingRequest(
        @NotEmpty(message = "input texts are required")
        List<String> input,
        String model,
        Integer dimensions
) {
    /** Convenience constructor for single-text embedding. */
    public EmbeddingRequest(String text, String model) {
        this(List.of(text), model, null);
    }
}
