package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.OAuth2ClientCredentialsAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractOpenAiCompatibleProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Baidu Qianfan (ERNIE) provider — OpenAI-compatible Chinese LLM with OAuth2 auth.
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://aip.baidubce.com/rpc/2.0/ai_custom/v1}
 *
 * <p>Auth: OAuth2 Client Credentials (API Key + Secret Key → access_token).
 * The v2 Qianfan API supports OpenAI chat/completions format.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.baidu-qianfan.api-key")
public class BaiduQianfanModelProvider extends AbstractOpenAiCompatibleProvider {

    private static final String PROVIDER_NAME = "baidu-qianfan";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public BaiduQianfanModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.baidu-qianfan.api-key:}") String apiKey,
            @Value("${app.ai.providers.baidu-qianfan.secret-key:}") String secretKey,
            @Value("${app.ai.providers.baidu-qianfan.base-url:https://qianfan.baidubce.com}") String baseUrl,
            @Value("${app.ai.providers.baidu-qianfan.token-url:https://aip.baidubce.com/oauth/2.0/token}") String tokenUrl,
            @Value("${app.ai.providers.baidu-qianfan.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.baidu-qianfan.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.baidu-qianfan.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.baidu-qianfan.supported-models:ERNIE-4.0-8K,ERNIE-3.5-8K,ERNIE-Speed-8K}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderBaiduQianfan"),
                new OAuth2ClientCredentialsAuth(tokenUrl, apiKey, secretKey, null),
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
        return "/v2/chat/completions";
    }
}
