package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.ResponsesRequest;
import com.ia.aggregator.application.ai.dto.ResponsesResult;

/**
 * Input port for structured responses (e.g., OpenAI Responses API).
 * Generates structured text outputs with tool-use capabilities.
 */
public interface ResponsesUseCase {
    ResponsesResult execute(ResponsesRequest request);
}
