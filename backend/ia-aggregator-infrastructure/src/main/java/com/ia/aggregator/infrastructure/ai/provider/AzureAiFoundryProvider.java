package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.AzureApiKeyAuthStrategy;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractOpenAiCompatibleProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Azure AI Foundry provider -- OpenAI-compatible models via Azure AI model catalog.
 *
 * <p>API: {@code https://{endpoint}.models.ai.azure.com}
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>Auth: Azure API key (api-key header)
 *
 * <p>Uses OpenAI-compatible format via AbstractOpenAiCompatibleProvider.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.azure-ai-foundry.api-key")
public class AzureAiFoundryProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "azure-ai-foundry";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    public AzureAiFoundryProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.azure-ai-foundry.api-key:}") String apiKey,
            @Value("${app.ai.providers.azure-ai-foundry.endpoint:}") String endpoint,
            @Value("${app.ai.providers.azure-ai-foundry.base-url:}") String baseUrlOverride,
            @Value("${app.ai.providers.azure-ai-foundry.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.azure-ai-foundry.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.azure-ai-foundry.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.azure-ai-foundry.supported-models:}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderAzureAiFoundry"),
                new AzureApiKeyAuthStrategy(apiKey),
                baseUrlOverride != null && !baseUrlOverride.isBlank()
                        ? baseUrlOverride
                        : "https://" + endpoint + ".models.ai.azure.com",
                timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    protected String chatEndpointPath() {
        return "/v1/chat/completions";
    }

    @Override
    protected String embeddingEndpointPath() {
        return "/v1/embeddings";
    }
}
