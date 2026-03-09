package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.domain.ai.Capability;

import java.util.List;

/**
 * Port for capability-based provider routing.
 *
 * <p>Decouples the application layer from the infrastructure routing logic,
 * allowing use cases to resolve providers by capability without depending
 * on concrete router implementations.
 *
 * <p>Design: Port (Hexagonal Architecture) — defines the contract that
 * the infrastructure's CapabilityRouter implements.
 */
public interface CapabilityRoutingPort {

    /**
     * Resolves the best provider for a given capability and optional model preference.
     *
     * @param capability     the required capability
     * @param preferredModel optional preferred model (null for default routing)
     * @return the resolved provider
     */
    MultiCapabilityProvider resolve(Capability capability, String preferredModel);

    /**
     * Returns all providers for a capability in priority order for fallback iteration.
     *
     * @param capability the required capability
     * @return list of providers supporting the capability
     */
    List<MultiCapabilityProvider> resolveAll(Capability capability);
}
