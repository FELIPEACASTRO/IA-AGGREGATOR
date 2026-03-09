package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A document to be re-ranked by relevance.
 *
 * @param id document identifier (for correlation in results)
 * @param text document text content (required)
 */
public record RerankDocument(
        String id,
        @NotBlank(message = "document text is required")
        String text
) {
}
