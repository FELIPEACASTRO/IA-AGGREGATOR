package com.ia.aggregator.domain.platform;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed per-request trace for observability.
 */
public record RequestTrace(
        UUID id,
        UUID orgId,
        UUID virtualKeyId,
        String capability,
        String requestedModel,
        String usedModel,
        String provider,
        int statusCode,
        long inputTokens,
        long outputTokens,
        double costUsd,
        long totalLatencyMs,
        long providerLatencyMs,
        long routingLatencyMs,
        boolean cacheHit,
        boolean fallbackUsed,
        String errorCode,
        String errorMessage,
        Instant timestamp
) {}
