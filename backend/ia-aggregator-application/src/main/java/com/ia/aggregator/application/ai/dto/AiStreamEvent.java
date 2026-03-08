package com.ia.aggregator.application.ai.dto;

public record AiStreamEvent(
        AiStreamEventType type,
        String delta,
        boolean done,
        AiUsageEstimate usage,
        AiCostEstimate estimatedCost,
        String providerUsed,
        String modelUsed,
        String requestId,
        String finishReason
) {
    public static AiStreamEvent start(String providerUsed, String modelUsed, String requestId) {
        return new AiStreamEvent(AiStreamEventType.START, null, false, null, null, providerUsed, modelUsed, requestId, null);
    }

    public static AiStreamEvent delta(String providerUsed, String modelUsed, String requestId, String delta) {
        return new AiStreamEvent(AiStreamEventType.DELTA, delta, false, null, null, providerUsed, modelUsed, requestId, null);
    }

    public static AiStreamEvent complete(String providerUsed,
                                         String modelUsed,
                                         String requestId,
                                         AiUsageEstimate usage,
                                         AiCostEstimate estimatedCost,
                                         String finishReason) {
        return new AiStreamEvent(AiStreamEventType.COMPLETE, null, true, usage, estimatedCost, providerUsed, modelUsed, requestId, finishReason);
    }

    public static AiStreamEvent error(String providerUsed, String modelUsed, String requestId, String finishReason) {
        return new AiStreamEvent(AiStreamEventType.ERROR, null, true, null, null, providerUsed, modelUsed, requestId, finishReason);
    }
}
