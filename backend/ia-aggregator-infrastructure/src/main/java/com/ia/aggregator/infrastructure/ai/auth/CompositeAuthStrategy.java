package com.ia.aggregator.infrastructure.ai.auth;

import java.net.http.HttpRequest;
import java.util.List;

/**
 * Composite authentication strategy that applies multiple strategies in sequence.
 *
 * <p>Use when a provider requires more than one authentication mechanism simultaneously.
 * Example: Azure OpenAI with api-key header AND api-version query param needs both
 * {@link AzureApiKeyAuthStrategy} and a custom query-param strategy applied together.
 *
 * <p>Pattern: Composite (GoF) — clients treat this the same as a single {@link AuthStrategy}.
 * Big O: O(N) where N = number of composed strategies; N is always small (typically 2).
 */
public class CompositeAuthStrategy implements AuthStrategy {

    private final List<AuthStrategy> strategies;

    /**
     * Creates a composite that applies the given strategies in order.
     *
     * @param strategies one or more strategies to apply sequentially; must not be empty
     */
    public CompositeAuthStrategy(List<AuthStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            throw new IllegalArgumentException("CompositeAuthStrategy requires at least one strategy");
        }
        this.strategies = List.copyOf(strategies);
    }

    /** Convenience constructor for exactly two strategies. */
    public CompositeAuthStrategy(AuthStrategy first, AuthStrategy second) {
        this(List.of(first, second));
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        HttpRequest.Builder current = builder;
        for (AuthStrategy strategy : strategies) {
            current = strategy.apply(current);
        }
        return current;
    }
}
