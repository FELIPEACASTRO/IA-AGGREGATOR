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
 * Reka AI provider — OpenAI-compatible multimodal LLM.
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://api.reka.ai/v1}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.reka.api-key")
public class RekaAiModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "reka";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public RekaAiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.reka.api-key:}") String apiKey,
            @Value("${app.ai.providers.reka.base-url:https://api.reka.ai}") String baseUrl,
            @Value("${app.ai.providers.reka.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.reka.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.reka.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.reka.supported-models:reka-core,reka-flash,reka-edge}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderReka"),
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
}
