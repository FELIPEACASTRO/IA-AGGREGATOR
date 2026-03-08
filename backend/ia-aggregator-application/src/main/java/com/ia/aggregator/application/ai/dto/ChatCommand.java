package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record ChatCommand(
        @NotBlank(message = "prompt is required")
        String prompt,
        String preferredModel,
        String provider,
        String systemPrompt,
        Double temperature,
        Integer maxTokens,
        Boolean stream,
        Map<String, String> metadata,
        List<String> fallbackProviders
) {
    public ChatCommand(String prompt, String preferredModel) {
        this(prompt, preferredModel, null, null, null, null, Boolean.FALSE, Map.of(), List.of());
    }

    public boolean streamRequested() {
        return Boolean.TRUE.equals(stream);
    }
}
