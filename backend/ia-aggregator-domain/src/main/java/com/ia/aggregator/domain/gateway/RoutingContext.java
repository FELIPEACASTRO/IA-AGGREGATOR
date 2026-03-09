package com.ia.aggregator.domain.gateway;

import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.domain.ai.RoutingStrategy;

import java.util.Map;
import java.util.UUID;

/**
 * Contextual information for routing a request.
 *
 * @param requestId      Unique request identifier
 * @param capability     The capability being requested
 * @param preferredModel User-preferred model (nullable)
 * @param strategy       Override strategy (nullable, uses rule-based otherwise)
 * @param userId         Authenticated user ID (nullable for anonymous)
 * @param orgId          Organization ID for policy evaluation
 * @param userRole       User role for policy matching
 * @param metadata       Additional context (headers, tags, etc.)
 */
public record RoutingContext(
        UUID requestId,
        Capability capability,
        String preferredModel,
        RoutingStrategy strategy,
        UUID userId,
        UUID orgId,
        String userRole,
        Map<String, String> metadata
) {
    public RoutingContext {
        if (requestId == null) requestId = UUID.randomUUID();
        if (metadata == null) metadata = Map.of();
    }

    public static RoutingContext of(Capability capability, String preferredModel) {
        return new RoutingContext(UUID.randomUUID(), capability, preferredModel,
                null, null, null, null, Map.of());
    }
}
