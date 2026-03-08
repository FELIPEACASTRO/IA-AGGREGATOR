package com.ia.aggregator.application.ai.dto;

public record AiUsageEstimate(
        long inputTokens,
        long outputTokens,
        long totalTokens
) {
    public static AiUsageEstimate empty() {
        return new AiUsageEstimate(0, 0, 0);
    }

    public static AiUsageEstimate of(long inputTokens, long outputTokens) {
        return new AiUsageEstimate(inputTokens, outputTokens, inputTokens + outputTokens);
    }
}
