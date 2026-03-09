package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Web grounding context returned by providers that augment responses with real-time search.
 *
 * <p>Used by: Perplexity Sonar ({@code citations} + {@code search_metadata}),
 * Gemini with search grounding, and any other WEB_GROUNDED_CHAT provider.
 *
 * @param citations     list of source citations; never null, may be empty
 * @param groundingSource  human-readable description of the grounding source
 *                        (e.g., {@code "Perplexity search"}, {@code "Google search"})
 */
public record GroundingMetadata(
        List<CitationMetadata> citations,
        String groundingSource
) {
    /** Empty grounding — for providers that do not return citations. */
    public static final GroundingMetadata EMPTY = new GroundingMetadata(List.of(), null);
}
