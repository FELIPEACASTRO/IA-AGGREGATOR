package com.ia.aggregator.application.ai.dto;

/**
 * A single citation/source reference returned by grounded chat providers
 * (Perplexity Sonar, Gemini with grounding, etc.).
 *
 * @param title   page or document title (may be null if provider omits it)
 * @param url     source URL (always present when citation is available)
 * @param snippet short excerpt from the cited source (may be null)
 * @param index   zero-based citation index as referenced in the response text (may be null)
 */
public record CitationMetadata(
        String title,
        String url,
        String snippet,
        Integer index
) {
}
