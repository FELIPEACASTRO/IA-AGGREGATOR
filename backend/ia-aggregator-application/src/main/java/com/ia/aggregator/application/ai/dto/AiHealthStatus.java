package com.ia.aggregator.application.ai.dto;

import java.time.Instant;
import java.util.List;

public record AiHealthStatus(
        String providerId,
        String providerName,
        boolean configured,
        boolean enabled,
        boolean reachable,
        String circuitState,
        Instant lastCheckedAt,
        Long latencyMs,
        String lastErrorCode,
        boolean supportsStreaming,
        String defaultModel,
        List<String> supportedModels,
        List<String> requiredSecrets
) {
}
