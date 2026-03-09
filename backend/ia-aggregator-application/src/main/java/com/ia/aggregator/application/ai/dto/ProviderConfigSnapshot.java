package com.ia.aggregator.application.ai.dto;

import com.ia.aggregator.domain.ai.AuthType;
import com.ia.aggregator.domain.ai.ProviderType;
import com.ia.aggregator.domain.ai.SyncMode;

/**
 * Immutable snapshot of a provider's runtime configuration.
 *
 * <p>Returned by the catalog and health endpoints to expose provider metadata
 * without exposing secrets. API keys are NEVER included here.
 *
 * @param providerName unique provider identifier (e.g., {@code "openai"})
 * @param defaultModel  the model used when no model is specified in the request
 * @param providerType  architectural group of the provider
 * @param authType      authentication strategy in use
 * @param syncMode      how the provider's primary capability completes
 * @param baseUrl       the provider's API base URL (for diagnostic purposes)
 * @param timeoutMs     configured HTTP request timeout in milliseconds
 */
public record ProviderConfigSnapshot(
        String providerName,
        String defaultModel,
        ProviderType providerType,
        AuthType authType,
        SyncMode syncMode,
        String baseUrl,
        long timeoutMs
) {
}
