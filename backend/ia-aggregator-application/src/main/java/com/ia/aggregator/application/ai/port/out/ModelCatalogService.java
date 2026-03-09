package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.application.ai.dto.ModelInfo;
import com.ia.aggregator.domain.ai.Capability;

import java.util.List;

/**
 * Port for querying the model catalog — aggregates model metadata from all registered providers.
 *
 * <p>Used by {@code AiProviderCatalogController} to expose provider and model listings.
 * Big O: O(P) where P = number of registered providers.
 */
public interface ModelCatalogService {

    /**
     * Returns all models from all registered providers.
     *
     * @return immutable list of model info sorted by provider name, then model name
     */
    List<ModelInfo> listAll();

    /**
     * Returns models that support the given capability.
     *
     * @param capability the capability to filter by
     * @return models supporting the capability, may be empty
     */
    List<ModelInfo> findByCapability(Capability capability);

    /**
     * Returns all models registered under the given provider name.
     *
     * @param providerName the exact provider name (case-sensitive)
     * @return models for that provider, empty if not found
     */
    List<ModelInfo> findByProvider(String providerName);
}
