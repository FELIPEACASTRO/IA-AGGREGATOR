package com.ia.aggregator.application.ai.dto;

public record ChatResponse(
        String content,
        String modelUsed,
        String providerUsed,
        boolean fallbackUsed,
        int attempts,
        String requestId,
        AiUsageEstimate usage,
        AiCostEstimate estimatedCost,
        long latencyMs,
        String finishReason
) {
    public ChatResponse(String content, String modelUsed, String providerUsed, boolean fallbackUsed, int attempts) {
        this(content, modelUsed, providerUsed, fallbackUsed, attempts, null, AiUsageEstimate.empty(), AiCostEstimate.unsupported(providerUsed, modelUsed), 0L, "completed");
    }
}
