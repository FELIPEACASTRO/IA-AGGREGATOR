package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.domain.ai.AuthType;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.domain.ai.ProviderStatus;
import com.ia.aggregator.domain.ai.ProviderType;
import com.ia.aggregator.domain.ai.SyncMode;

import java.util.Set;

/**
 * SPI interface that every {@code MultiCapabilityProvider} must implement to expose
 * metadata about itself to the registry, catalog, and observability infrastructure.
 *
 * <p>Pattern: Strategy + Descriptor — routing decisions use the descriptor to filter
 * eligible providers before delegating to a specific capability interface.
 *
 * <p>Big O: All methods O(1) — implementations return pre-computed constants.
 */
public interface ProviderDescriptor {

    /** Unique machine-readable provider name (e.g., {@code "openai"}, {@code "anthropic"}). */
    String providerName();

    /** Architectural group for routing and base-class selection. */
    ProviderType providerType();

    /** Authentication strategy this provider uses. */
    AuthType authType();

    /**
     * How this provider's primary capability completes.
     * Providers with mixed sync/async capabilities should return the most common mode;
     * per-capability sync mode is expressed in {@code ProviderConfigSnapshot}.
     */
    SyncMode syncMode();

    /** Complete set of AI capabilities this provider supports. */
    Set<Capability> capabilities();

    /** Current implementation and operational status. */
    ProviderStatus status();

    /** URL to official API documentation (for human reference). */
    String docsUrl();

    /** URL where developers can obtain an API key (for human reference). */
    String apiKeyUrl();
}
