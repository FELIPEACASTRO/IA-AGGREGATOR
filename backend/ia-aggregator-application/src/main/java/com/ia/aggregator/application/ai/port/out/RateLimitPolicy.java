package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.domain.ai.Capability;

/**
 * Port that checks whether a request is allowed under the configured rate limit
 * for a given provider and capability combination.
 *
 * <p>Backed by Resilience4j RateLimiter instances configured per provider.
 * Complements the circuit breaker — rate limiting is proactive (capacity-based),
 * while circuit breaking is reactive (failure-based).
 *
 * <p>Big O: O(1) per check — Resilience4j uses AtomicReference internally.
 */
public interface RateLimitPolicy {

    /**
     * Returns {@code true} if the request is within the configured rate limit
     * for the provider/capability pair; {@code false} if it should be rejected.
     *
     * <p>Implementations must not block; they must return immediately.
     *
     * @param providerName the provider to check (e.g., {@code "openai"})
     * @param capability   the capability being invoked
     * @return {@code true} if allowed, {@code false} if rate-limited
     */
    boolean isAllowed(String providerName, Capability capability);
}
