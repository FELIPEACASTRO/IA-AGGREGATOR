package com.ia.aggregator.application.ai.dto;

public record ChatResponse(
        String content,
        String modelUsed,
        String providerUsed,
        boolean fallbackUsed,
        int attempts
) {
}
