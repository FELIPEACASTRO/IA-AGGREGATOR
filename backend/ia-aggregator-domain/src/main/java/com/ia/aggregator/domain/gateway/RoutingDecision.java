package com.ia.aggregator.domain.gateway;

import com.ia.aggregator.domain.ai.RoutingStrategy;

import java.util.List;

/**
 * The result of a routing decision — an ordered list of provider/model candidates.
 *
 * @param candidates     Ordered list of provider+model candidates (best first)
 * @param appliedRule    The rule that produced this decision (nullable if default)
 * @param strategy       The effective strategy applied
 * @param cacheHit       Whether a semantic cache hit was used
 */
public record RoutingDecision(
        List<ProviderCandidate> candidates,
        String appliedRule,
        RoutingStrategy strategy,
        boolean cacheHit
) {
    /**
     * A single provider+model candidate.
     *
     * @param providerName Provider identifier
     * @param model        Model identifier
     * @param weight       Weight for traffic splitting (0-100)
     * @param estimatedCostUsd Estimated cost per 1K tokens
     * @param estimatedLatencyMs Estimated P50 latency
     */
    public record ProviderCandidate(
            String providerName,
            String model,
            int weight,
            double estimatedCostUsd,
            long estimatedLatencyMs
    ) {}
}
