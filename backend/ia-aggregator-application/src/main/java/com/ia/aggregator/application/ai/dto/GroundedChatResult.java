package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Result DTO for web-grounded chat.
 *
 * @param content LLM-generated answer grounded in web sources
 * @param citations list of source citations used
 * @param modelUsed actual model used
 * @param providerUsed provider that generated the response
 */
public record GroundedChatResult(
        String content,
        List<Citation> citations,
        String modelUsed,
        String providerUsed
) {
    /**
     * A source citation from web search.
     *
     * @param title source title
     * @param url source URL
     * @param snippet relevant excerpt from the source
     */
    public record Citation(String title, String url, String snippet) {
    }
}
