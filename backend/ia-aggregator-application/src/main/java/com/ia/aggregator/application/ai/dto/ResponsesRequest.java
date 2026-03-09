package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for structured responses with tool use (OpenAI Responses API format).
 *
 * @param input the user input or conversation context (required)
 * @param model preferred model (optional)
 * @param instructions system-level instructions (optional)
 * @param tools list of tool definitions (optional)
 * @param temperature sampling temperature (optional)
 * @param maxOutputTokens maximum output tokens (optional)
 */
public record ResponsesRequest(
        @NotBlank(message = "input is required")
        String input,
        String model,
        String instructions,
        List<Map<String, Object>> tools,
        Double temperature,
        Integer maxOutputTokens
) {
    /** Convenience constructor for simple responses request. */
    public ResponsesRequest(String input, String model) {
        this(input, model, null, null, null, null);
    }
}
