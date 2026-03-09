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
 * Zhipu AI (BigModel / GLM) provider — OpenAI-compatible Chinese LLM.
 *
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>API: {@code https://open.bigmodel.cn/api/paas/v4}
 *
 * <p>Zhipu supports JWT auth but also accepts plain Bearer token with API key.
 * The v4 API uses OpenAI-compatible chat/completions format.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.zhipu.api-key")
public class ZhipuAiModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "zhipu";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    public ZhipuAiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.zhipu.api-key:}") String apiKey,
            @Value("${app.ai.providers.zhipu.base-url:https://open.bigmodel.cn/api/paas}") String baseUrl,
            @Value("${app.ai.providers.zhipu.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.zhipu.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.zhipu.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.zhipu.supported-models:glm-4-plus,glm-4,glm-4-flash}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderZhipu"),
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
        return "/v4/chat/completions";
    }

    @Override
    protected String embeddingEndpointPath() {
        return "/v4/embeddings";
    }

    @Override
    protected String resolveDefaultEmbeddingModel() {
        return "embedding-3";
    }
}
