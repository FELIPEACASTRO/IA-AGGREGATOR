package com.ia.aggregator.application.ai.dto;

import java.util.List;

public record AiPromptResponse(
        String content,
        String providerUsed,
        String modelUsed,
        String requestId,
        AiUsageEstimate usage,
        AiCostEstimate estimatedCost,
        long latencyMs,
        String finishReason,
        List<String> citations
) {
    public AiPromptResponse {
        usage = usage == null ? AiUsageEstimate.empty() : usage;
        estimatedCost = estimatedCost == null ? AiCostEstimate.unsupported(providerUsed, modelUsed) : estimatedCost;
        finishReason = finishReason == null || finishReason.isBlank() ? "completed" : finishReason;
        citations = citations == null ? List.of() : List.copyOf(citations);
    }

    public AiPromptResponse(String content, String providerUsed, String modelUsed, String requestId) {
        this(content, providerUsed, modelUsed, requestId, AiUsageEstimate.empty(), AiCostEstimate.unsupported(providerUsed, modelUsed), 0L, "completed", List.of());
    }
}
