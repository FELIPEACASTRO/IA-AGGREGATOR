package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractOpenAiCompatibleProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * GitHub Models provider — Azure-hosted OpenAI-compatible inference.
 *
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>Models: GPT-4o, Llama-3.3, Mistral, and others via GitHub token.
 * <p>API: {@code https://models.inference.ai.azure.com}
 *
 * <p>Only registered when a GitHub token is configured.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.github-models.api-key")
public class GitHubModelsProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "github-models";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    public GitHubModelsProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.github-models.api-key:}") String apiKey,
            @Value("${app.ai.providers.github-models.base-url:https://models.inference.ai.azure.com}") String baseUrl,
            @Value("${app.ai.providers.github-models.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.github-models.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.github-models.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.github-models.supported-models:gpt-4o,Meta-Llama-3.1-405B-Instruct,Mistral-large-2411}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderGithubModels"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<Capability> capabilities() {
        return SUPPORTED_CAPABILITIES;
    }

    @Override
    protected String chatEndpointPath() {
        return "/chat/completions"; // GitHub Models uses /chat/completions without /v1 prefix
    }

    @Override
    protected String embeddingEndpointPath() {
        return "/embeddings";
    }

    @Override
    protected String resolveDefaultEmbeddingModel() {
        return "text-embedding-3-small";
    }
}
