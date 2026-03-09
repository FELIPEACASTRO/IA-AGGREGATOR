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
 * Alibaba DashScope provider -- OpenAI-compatible API for Qwen models.
 *
 * <p>API: {@code https://dashscope.aliyuncs.com/compatible-mode}
 * <p>Supports: CHAT
 * <p>Auth: Bearer token
 *
 * <p>Uses OpenAI-compatible format via AbstractOpenAiCompatibleProvider.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.alibaba-dashscope.api-key")
public class AlibabaDashScopeProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "alibaba-dashscope";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT);

    public AlibabaDashScopeProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.alibaba-dashscope.api-key:}") String apiKey,
            @Value("${app.ai.providers.alibaba-dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode}") String baseUrl,
            @Value("${app.ai.providers.alibaba-dashscope.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.alibaba-dashscope.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.alibaba-dashscope.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.alibaba-dashscope.supported-models:qwen-max,qwen-plus,qwen-turbo}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderAlibabaDashScope"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
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
