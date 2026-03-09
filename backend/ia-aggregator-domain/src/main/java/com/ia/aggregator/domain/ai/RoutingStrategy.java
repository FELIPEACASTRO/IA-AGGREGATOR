package com.ia.aggregator.domain.ai;

/**
 * Routing strategies for provider selection.
 *
 * <p>Each strategy defines how the gateway selects providers/models
 * for a given request based on different optimization goals.
 */
public enum RoutingStrategy {

    /** Minimize cost per token (cheapest model first). */
    COST_OPTIMIZED,

    /** Minimize response latency (fastest provider first). */
    LATENCY_OPTIMIZED,

    /** Maximize output quality (best model first). */
    QUALITY_OPTIMIZED,

    /** Strict compliance — only use policy-approved models. */
    POLICY_STRICT,

    /** Balanced between cost, latency, and quality (default). */
    BALANCED_DEFAULT
}
