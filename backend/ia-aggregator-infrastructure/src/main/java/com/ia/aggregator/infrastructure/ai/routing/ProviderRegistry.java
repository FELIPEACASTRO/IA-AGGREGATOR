package com.ia.aggregator.infrastructure.ai.routing;

import com.ia.aggregator.application.ai.dto.ModelInfo;
import com.ia.aggregator.application.ai.dto.ProviderHealth;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.domain.ai.Capability;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for all AI providers. Supports capability-based routing.
 *
 * <p>Design Pattern: Factory — provides provider discovery and lookup by capability.
 * <p>Big O: O(1) amortized for capability lookup via HashMap; O(N) for full catalog.
 * <p>Thread-safe: uses ConcurrentHashMap for runtime caching.
 */
@Component
public class ProviderRegistry {

    private final List<MultiCapabilityProvider> allProviders;
    private final Map<Capability, List<MultiCapabilityProvider>> providersByCapability;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ProviderRegistry(
            List<MultiCapabilityProvider> allProviders,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.allProviders = Collections.unmodifiableList(allProviders);
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.providersByCapability = buildCapabilityIndex(allProviders);
    }

    /**
     * Returns all providers that support the given capability.
     * O(1) amortized via pre-built index.
     */
    public List<MultiCapabilityProvider> getProviders(Capability capability) {
        return providersByCapability.getOrDefault(capability, Collections.emptyList());
    }

    /**
     * Returns a specific provider by name that supports the given capability.
     */
    public Optional<MultiCapabilityProvider> getProvider(String providerName, Capability capability) {
        return getProviders(capability).stream()
                .filter(p -> p.providerName().equals(providerName))
                .findFirst();
    }

    /**
     * Returns a specific provider by name (regardless of capability).
     */
    public Optional<MultiCapabilityProvider> getProviderByName(String providerName) {
        return allProviders.stream()
                .filter(p -> p.providerName().equals(providerName))
                .findFirst();
    }

    /**
     * Returns all registered providers.
     */
    public List<MultiCapabilityProvider> getAllProviders() {
        return allProviders;
    }

    /**
     * Returns health status for a specific provider.
     */
    public ProviderHealth healthCheck(String providerName) {
        return allProviders.stream()
                .filter(p -> p.providerName().equals(providerName))
                .findFirst()
                .map(this::buildHealthStatus)
                .orElse(new ProviderHealth(providerName, "UNKNOWN", Set.of(),
                        null, null, "UNKNOWN", null));
    }

    /**
     * Returns the full model/capability catalog.
     * O(N*M) where N = providers, M = models per provider.
     */
    public List<ModelInfo> catalog() {
        List<ModelInfo> catalog = new ArrayList<>();
        for (MultiCapabilityProvider provider : allProviders) {
            // Each provider exposes its supported models through the supports() method
            catalog.add(new ModelInfo(
                    provider.providerName(),
                    provider.providerName(),
                    provider.capabilities(),
                    null,
                    "standard"
            ));
        }
        return catalog;
    }

    /**
     * Returns health status for all providers.
     * O(N) where N = total providers.
     */
    public List<ProviderHealth> healthCheckAll() {
        return allProviders.stream()
                .map(this::buildHealthStatus)
                .toList();
    }

    /**
     * Returns all supported capabilities across all providers.
     */
    public Set<Capability> allCapabilities() {
        return providersByCapability.keySet();
    }

    /**
     * Returns the count of providers in each state.
     */
    public Map<String, Long> healthSummary() {
        return healthCheckAll().stream()
                .collect(Collectors.groupingBy(ProviderHealth::status, Collectors.counting()));
    }

    // ---------- Internal ----------

    private Map<Capability, List<MultiCapabilityProvider>> buildCapabilityIndex(
            List<MultiCapabilityProvider> providers) {
        Map<Capability, List<MultiCapabilityProvider>> index = new EnumMap<>(Capability.class);
        for (MultiCapabilityProvider provider : providers) {
            for (Capability capability : provider.capabilities()) {
                index.computeIfAbsent(capability, k -> new ArrayList<>()).add(provider);
            }
        }
        // Make lists immutable
        index.replaceAll((k, v) -> Collections.unmodifiableList(v));
        return Collections.unmodifiableMap(index);
    }

    private ProviderHealth buildHealthStatus(MultiCapabilityProvider provider) {
        String cbName = "aiProvider" + capitalize(provider.providerName());
        String cbState;
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(cbName);
            cbState = cb.getState().name();
        } catch (Exception e) {
            cbState = "UNKNOWN";
        }

        String status = switch (cbState) {
            case "CLOSED" -> "UP";
            case "OPEN" -> "DOWN";
            case "HALF_OPEN" -> "DEGRADED";
            default -> "UNKNOWN";
        };

        return new ProviderHealth(
                provider.providerName(),
                status,
                provider.capabilities(),
                null, null,
                cbState,
                null
        );
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
