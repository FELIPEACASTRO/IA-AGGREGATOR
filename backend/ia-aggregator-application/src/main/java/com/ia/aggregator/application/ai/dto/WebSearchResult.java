package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Result DTO for web search.
 *
 * @param hits list of search result hits
 * @param totalResults total number of results available
 * @param modelUsed engine/model used
 * @param providerUsed provider that performed the search
 */
public record WebSearchResult(
        List<SearchHit> hits,
        Integer totalResults,
        String modelUsed,
        String providerUsed
) {
}
