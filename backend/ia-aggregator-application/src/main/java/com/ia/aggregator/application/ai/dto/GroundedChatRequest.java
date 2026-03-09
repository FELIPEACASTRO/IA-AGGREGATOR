package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for web-grounded chat.
 *
 * @param query the user's question to answer with web-grounded context (required)
 * @param model preferred model (optional)
 * @param systemPrompt optional system-level instruction
 * @param maxSearchResults max web results to include as context (optional)
 * @param temperature sampling temperature (optional)
 */
public record GroundedChatRequest(
        @NotBlank(message = "query is required")
        String query,
        String model,
        String systemPrompt,
        Integer maxSearchResults,
        Double temperature
) {
    /** Convenience constructor for simple grounded chat. */
    public GroundedChatRequest(String query, String model) {
        this(query, model, null, null, null);
    }
}
