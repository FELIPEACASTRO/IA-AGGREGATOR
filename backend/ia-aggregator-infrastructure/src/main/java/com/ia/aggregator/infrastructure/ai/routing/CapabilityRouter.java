package com.ia.aggregator.infrastructure.ai.routing;

import com.ia.aggregator.application.ai.port.out.CapabilityRoutingPort;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Routes requests to the appropriate provider based on capability and optional model preference.
 *
 * <p>Implements {@link CapabilityRoutingPort} to satisfy the application layer's
 * hexagonal architecture port contract.
 *
 * <p>Design Pattern: Strategy — routing strategy can be extended for weighted routing,
 * geographic affinity, cost optimization, etc.
 *
 * <p>Big O: O(P) where P = providers supporting the capability. O(1) amortized
 * when the preferred provider matches on first try.
 */
@Component
public class CapabilityRouter implements CapabilityRoutingPort {

    private final ProviderRegistry providerRegistry;

    public CapabilityRouter(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    /**
     * Resolves the best provider for a given capability and optional model preference.
     *
     * @param capability the required capability
     * @param preferredModel optional preferred model (null for default routing)
     * @return the resolved provider
     * @throws TechnicalException if no provider supports the capability/model
     */
    public MultiCapabilityProvider resolve(Capability capability, String preferredModel) {
        List<MultiCapabilityProvider> providers = providerRegistry.getProviders(capability);

        if (providers.isEmpty()) {
            throw new TechnicalException(ErrorCode.AI_001,
                    "No provider available for capability: " + capability);
        }

        if (preferredModel != null && !preferredModel.isBlank()) {
            // Try to find a provider that supports the preferred model
            return providers.stream()
                    .filter(p -> p.supports(preferredModel))
                    .findFirst()
                    .orElse(providers.get(0)); // Fallback to first available
        }

        return providers.get(0); // Default: first provider for the capability
    }

    /**
     * Returns all providers for a capability in priority order for fallback iteration.
     */
    public List<MultiCapabilityProvider> resolveAll(Capability capability) {
        List<MultiCapabilityProvider> providers = providerRegistry.getProviders(capability);
        if (providers.isEmpty()) {
            throw new TechnicalException(ErrorCode.AI_001,
                    "No provider available for capability: " + capability);
        }
        return providers;
    }
}
