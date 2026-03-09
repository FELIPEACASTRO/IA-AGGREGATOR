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
 * DeepInfra provider — OpenAI-compatible API for open-source models.
 *
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>Models: Llama-3.3, Mixtral, Qwen, and others.
 * <p>API: {@code https://api.deepinfra.com/v1/openai}
 *
 * <p>Only registered when an API key is configured.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.deepinfra.api-key")
public class DeepInfraModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "deepinfra";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    public DeepInfraModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.deepinfra.api-key:}") String apiKey,
            @Value("${app.ai.providers.deepinfra.base-url:https://api.deepinfra.com/v1/openai}") String baseUrl,
            @Value("${app.ai.providers.deepinfra.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.deepinfra.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.deepinfra.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.deepinfra.supported-models:meta-llama/Llama-3.3-70B-Instruct,mistralai/Mixtral-8x22B-Instruct-v0.1,Qwen/Qwen2.5-72B-Instruct}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderDeepinfra"),
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
        return "/chat/completions"; // DeepInfra baseUrl already includes /v1/openai
    }

    @Override
    protected String embeddingEndpointPath() {
        return "/embeddings";
    }

    @Override
    protected String resolveDefaultEmbeddingModel() {
        return "BAAI/bge-large-en-v1.5";
    }
}
