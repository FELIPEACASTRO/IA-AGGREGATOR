package com.ia.aggregator.infrastructure.ai.adapter;

import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring configuration that wraps all legacy {@link AiModelProvider} beans
 * as {@link MultiCapabilityProvider} beans via {@link LegacyProviderAdapter}.
 *
 * <p>This ensures all 17 existing providers are automatically available in the
 * new multi-capability system without any modifications to their code.
 */
@Configuration
public class LegacyProviderRegistrar {

    /**
     * Creates a list of adapted multi-capability providers from all legacy providers.
     * Only wraps providers that are NOT already MultiCapabilityProvider instances.
     */
    @Bean
    public List<MultiCapabilityProvider> legacyAdaptedProviders(List<AiModelProvider> legacyProviders) {
        return legacyProviders.stream()
                .filter(provider -> !(provider instanceof MultiCapabilityProvider))
                .map(LegacyProviderAdapter::new)
                .collect(Collectors.toList());
    }
}
