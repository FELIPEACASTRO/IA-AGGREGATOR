package com.ia.aggregator.domain.platform;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A virtual API key for developer platform access with scoped permissions.
 */
public record VirtualKey(
        UUID id,
        UUID orgId,
        UUID createdBy,
        String name,
        String keyHash,
        String keyPrefix,
        List<String> allowedModels,
        List<String> allowedCapabilities,
        int rateLimitRpm,
        double budgetLimitUsd,
        double spentUsd,
        boolean enabled,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt
) {}
