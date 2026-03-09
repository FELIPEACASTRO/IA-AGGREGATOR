package com.ia.aggregator.application.ai.dto;

/**
 * A single search result hit.
 *
 * @param title result title
 * @param url result URL
 * @param snippet text snippet / excerpt
 * @param score relevance score (optional, provider-dependent)
 * @param publishedDate publication date ISO-8601 (optional)
 */
public record SearchHit(
        String title,
        String url,
        String snippet,
        Double score,
        String publishedDate
) {
}
