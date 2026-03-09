package com.ia.aggregator.application.ai.dto;

import com.ia.aggregator.domain.ai.Capability;

import java.util.Set;

/**
 * Health status of an AI provider.
 *
 * @param providerName provider identifier
 * @param status health status: "UP", "DOWN", "DEGRADED"
 * @param capabilities set of capabilities this provider supports
 * @param latencyP50Ms p50 latency in milliseconds (null if unknown)
 * @param latencyP95Ms p95 latency in milliseconds (null if unknown)
 * @param circuitBreakerState circuit breaker state: "CLOSED", "OPEN", "HALF_OPEN"
 * @param errorRate error rate percentage [0.0, 100.0] (null if unknown)
 */
public record ProviderHealth(
        String providerName,
        String status,
        Set<Capability> capabilities,
        Long latencyP50Ms,
        Long latencyP95Ms,
        String circuitBreakerState,
        Double errorRate
) {
}
