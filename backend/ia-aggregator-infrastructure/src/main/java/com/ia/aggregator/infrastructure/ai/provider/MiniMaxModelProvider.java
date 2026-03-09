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
 * MiniMax provider — OpenAI-compatible Chinese LLM.
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://api.minimax.chat/v1}
 *
 * <p>MiniMax uses standard Bearer auth; group_id is passed as a query param
 * or header in certain legacy endpoints but the v1 chat/completions endpoint
 * uses pure OpenAI format with Bearer token.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.minimax.api-key")
public class MiniMaxModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "minimax";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public MiniMaxModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.minimax.api-key:}") String apiKey,
            @Value("${app.ai.providers.minimax.base-url:https://api.minimax.chat}") String baseUrl,
            @Value("${app.ai.providers.minimax.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.minimax.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.minimax.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.minimax.supported-models:abab6.5s-chat,abab6.5-chat,abab5.5-chat}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderMinimax"),
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
