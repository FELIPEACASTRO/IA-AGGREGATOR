package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the new multi-capability chat interface.
 * Extends the original ChatCommand with additional parameters.
 *
 * @param prompt the user prompt text (required)
 * @param model preferred model identifier (optional — routing decides if null)
 * @param systemPrompt optional system-level instruction
 * @param temperature sampling temperature [0.0, 2.0] (optional)
 * @param maxTokens maximum tokens in the response (optional)
 */
public record ChatRequest(
        @NotBlank(message = "prompt is required")
        String prompt,
        String model,
        String systemPrompt,
        Double temperature,
        Integer maxTokens
) {
    /** Convenience constructor for simple prompt-only requests. */
    public ChatRequest(String prompt, String model) {
        this(prompt, model, null, null, null);
    }
}
