package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.WebSearchRequest;
import com.ia.aggregator.application.ai.dto.WebSearchResult;

/**
 * Capability interface for web search with structured results.
 * Providers implementing this can search the web and return ranked results.
 */
public interface WebSearchCapable {

    /**
     * Performs a web search query.
     *
     * @param request the search request with query, filters, and pagination
     * @return the search result with ranked hits and metadata
     */
    WebSearchResult search(WebSearchRequest request);
}
