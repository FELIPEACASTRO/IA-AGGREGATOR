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
 * 01.AI (Yi) provider — OpenAI-compatible Chinese LLM.
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://api.lingyiwanwu.com/v1}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.zero-one-ai.api-key")
public class ZeroOneAiModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "01ai";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public ZeroOneAiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.zero-one-ai.api-key:}") String apiKey,
            @Value("${app.ai.providers.zero-one-ai.base-url:https://api.lingyiwanwu.com}") String baseUrl,
            @Value("${app.ai.providers.zero-one-ai.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.zero-one-ai.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.zero-one-ai.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.zero-one-ai.supported-models:yi-lightning,yi-large,yi-medium}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderZeroOneAi"),
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
