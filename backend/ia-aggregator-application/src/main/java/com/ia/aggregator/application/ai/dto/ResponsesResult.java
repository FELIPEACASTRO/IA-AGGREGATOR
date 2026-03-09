package com.ia.aggregator.application.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * Result DTO for structured responses (OpenAI Responses API format).
 *
 * @param output list of output items (text messages, tool calls, etc.)
 * @param modelUsed actual model used
 * @param providerUsed provider that generated the response
 * @param inputTokens input token count (optional)
 * @param outputTokens output token count (optional)
 */
public record ResponsesResult(
        List<Map<String, Object>> output,
        String modelUsed,
        String providerUsed,
        Integer inputTokens,
        Integer outputTokens
) {
}
