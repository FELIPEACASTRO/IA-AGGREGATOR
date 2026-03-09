package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.GroundedChatRequest;
import com.ia.aggregator.application.ai.dto.GroundedChatResult;

/**
 * Capability interface for chat grounded with real-time web search results.
 * Providers implementing this combine LLM responses with web-sourced citations.
 */
public interface WebGroundedChatCapable {

    /**
     * Executes a chat request grounded with real-time web search.
     *
     * @param request the grounded chat request with query and search parameters
     * @return the result with LLM response and source citations
     */
    GroundedChatResult groundedChat(GroundedChatRequest request);
}
