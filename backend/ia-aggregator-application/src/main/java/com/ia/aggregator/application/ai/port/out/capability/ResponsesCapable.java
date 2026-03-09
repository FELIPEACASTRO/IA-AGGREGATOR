package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.ResponsesRequest;
import com.ia.aggregator.application.ai.dto.ResponsesResult;

/**
 * Capability interface for structured responses with tool use and multi-turn context.
 * Supports OpenAI Responses API format.
 */
public interface ResponsesCapable {

    /**
     * Executes a structured response request with tool definitions and context.
     *
     * @param request the responses request containing instructions, tools, and input
     * @return the structured result with output items and tool calls
     */
    ResponsesResult responses(ResponsesRequest request);
}
