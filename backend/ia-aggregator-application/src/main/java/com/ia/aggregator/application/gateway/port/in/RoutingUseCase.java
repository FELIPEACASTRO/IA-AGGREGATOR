package com.ia.aggregator.application.gateway.port.in;

import com.ia.aggregator.domain.gateway.RoutingContext;
import com.ia.aggregator.domain.gateway.RoutingDecision;

/**
 * Use case for intelligent routing decisions.
 *
 * <p>Evaluates routing rules, applies strategy-based selection,
 * and returns ordered provider candidates.
 */
public interface RoutingUseCase {

    /**
     * Route a request to the best provider(s) based on context and rules.
     *
     * @param context the routing context with capability, preferences, and metadata
     * @return the routing decision with ordered candidates
     */
    RoutingDecision route(RoutingContext context);
}
