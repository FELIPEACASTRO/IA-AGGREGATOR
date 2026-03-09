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
 * SiliconFlow provider — OpenAI-compatible API hosted in China.
 *
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>Models: Qwen-Plus, DeepSeek-V3, GLM-4, and others.
 * <p>API: {@code https://api.siliconflow.cn/v1}
 *
 * <p>Only registered when an API key is configured.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.siliconflow.api-key")
public class SiliconFlowModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "siliconflow";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    public SiliconFlowModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.siliconflow.api-key:}") String apiKey,
            @Value("${app.ai.providers.siliconflow.base-url:https://api.siliconflow.cn}") String baseUrl,
            @Value("${app.ai.providers.siliconflow.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.siliconflow.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.siliconflow.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.siliconflow.supported-models:Qwen/Qwen2.5-72B-Instruct,deepseek-ai/DeepSeek-V3,THUDM/glm-4-9b-chat}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderSiliconflow"),
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
    protected String resolveDefaultEmbeddingModel() {
        return "BAAI/bge-large-en-v1.5";
    }
}
