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
 * SenseNova (SenseTime) provider — OpenAI-compatible Chinese LLM.
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://api.sensenova.cn/v1}
 *
 * <p>SenseNova v1 API accepts Bearer token with API key
 * and follows OpenAI chat/completions format.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.sensenova.api-key")
public class SenseNovaModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "sensenova";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public SenseNovaModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.sensenova.api-key:}") String apiKey,
            @Value("${app.ai.providers.sensenova.base-url:https://api.sensenova.cn}") String baseUrl,
            @Value("${app.ai.providers.sensenova.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.sensenova.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.sensenova.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.sensenova.supported-models:SenseChat-5,SenseChat-Turbo,SenseChat}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderSensenova"),
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
