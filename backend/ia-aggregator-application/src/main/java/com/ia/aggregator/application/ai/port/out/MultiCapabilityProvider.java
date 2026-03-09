package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.domain.ai.Capability;

import java.util.Set;

/**
 * Extension of {@link AiModelProvider} that supports multiple AI capabilities.
 *
 * <p>Providers implementing this interface declare which capabilities they support
 * (e.g., CHAT, EMBEDDINGS, IMAGE_GENERATION) and can be routed based on capability type.
 *
 * <p>Design Patterns:
 * <ul>
 *   <li>Strategy — each provider is a strategy for a set of capabilities</li>
 *   <li>Adapter — {@code LegacyProviderAdapter} wraps existing {@code AiModelProvider}
 *       implementations to conform to this interface without modification</li>
 * </ul>
 *
 * <p>SOLID: Open/Closed — new capabilities can be added via new sub-interfaces
 * without modifying existing provider implementations.
 */
public interface MultiCapabilityProvider extends AiModelProvider {

    /**
     * Returns the set of capabilities this provider supports.
     *
     * @return immutable set of supported capabilities; never null or empty
     */
    Set<Capability> capabilities();

    /**
     * Checks if this provider supports a specific capability.
     *
     * @param capability the capability to check
     * @return true if the provider can handle requests for the given capability
     */
    default boolean supportsCapability(Capability capability) {
        return capabilities().contains(capability);
    }
}
