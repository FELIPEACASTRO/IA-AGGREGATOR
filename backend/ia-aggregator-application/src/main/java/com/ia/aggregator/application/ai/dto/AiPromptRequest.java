package com.ia.aggregator.application.ai.dto;

import java.util.List;
import java.util.Map;

public record AiPromptRequest(
        String requestId,
        String prompt,
        String provider,
        String model,
        String systemPrompt,
        Double temperature,
        Integer maxTokens,
        boolean stream,
        Map<String, String> metadata,
        List<String> fallbackProviders
) {
    public AiPromptRequest(String requestId, String prompt, String provider, String model) {
        this(requestId, prompt, provider, model, null, null, null, false, Map.of(), List.of());
    }
}
