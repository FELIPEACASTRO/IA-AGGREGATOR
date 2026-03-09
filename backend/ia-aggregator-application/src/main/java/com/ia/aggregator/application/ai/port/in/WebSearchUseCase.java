package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.WebSearchRequest;
import com.ia.aggregator.application.ai.dto.WebSearchResult;

/**
 * Input port for web search.
 * Performs web searches and returns structured results.
 */
public interface WebSearchUseCase {
    WebSearchResult execute(WebSearchRequest request);
}
