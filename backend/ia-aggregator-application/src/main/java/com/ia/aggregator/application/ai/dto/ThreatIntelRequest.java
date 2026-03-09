package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for threat intelligence search.
 *
 * @param query the search query for threat intelligence (required)
 * @param model preferred provider/engine (optional)
 * @param dateFrom start date filter ISO-8601 (optional)
 * @param dateTo end date filter ISO-8601 (optional)
 * @param maxResults maximum results to return (optional)
 * @param sourceType filter by source type (e.g., "forum", "marketplace", "paste") (optional)
 */
public record ThreatIntelRequest(
        @NotBlank(message = "query is required")
        String query,
        String model,
        String dateFrom,
        String dateTo,
        Integer maxResults,
        String sourceType
) {
    /** Convenience constructor for simple threat intel search. */
    public ThreatIntelRequest(String query) {
        this(query, null, null, null, null, null);
    }
}
