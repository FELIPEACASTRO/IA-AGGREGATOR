package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for web search.
 *
 * @param query the search query (required)
 * @param model preferred search model/engine (optional)
 * @param maxResults maximum number of results to return (optional)
 * @param dateRange date filter (e.g., "past_day", "past_week", "past_month") (optional)
 * @param domains comma-separated domain filter (optional)
 */
public record WebSearchRequest(
        @NotBlank(message = "query is required")
        String query,
        String model,
        Integer maxResults,
        String dateRange,
        String domains
) {
    /** Convenience constructor for simple search. */
    public WebSearchRequest(String query) {
        this(query, null, null, null, null);
    }
}
