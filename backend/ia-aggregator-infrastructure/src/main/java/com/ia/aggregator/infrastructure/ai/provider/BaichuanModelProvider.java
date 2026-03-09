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
 * Baichuan provider — OpenAI-compatible Chinese LLM.
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://api.baichuan-ai.com/v1}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.baichuan.api-key")
public class BaichuanModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "baichuan";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public BaichuanModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.baichuan.api-key:}") String apiKey,
            @Value("${app.ai.providers.baichuan.base-url:https://api.baichuan-ai.com}") String baseUrl,
            @Value("${app.ai.providers.baichuan.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.baichuan.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.baichuan.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.baichuan.supported-models:Baichuan4,Baichuan3-Turbo,Baichuan2-Turbo}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderBaichuan"),
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
