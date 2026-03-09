package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.ChatRequest;
import com.ia.aggregator.application.ai.dto.ChatResult;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.CompositeAuthStrategy;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractNativeLlmProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Naver CLOVA Studio provider — Korean enterprise LLM (native API format).
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://clovastudio.stream.ntruss.com}
 *
 * <p>Auth: X-NCP-CLOVASTUDIO-API-KEY + X-NCP-APIGW-API-KEY headers.
 * Request/response format is CLOVA-native, not OpenAI-compatible.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.naver-clova.api-key")
public class NaverClovaModelProvider extends AbstractNativeLlmProvider {

    private static final String PROVIDER_NAME = "naver-clova";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public NaverClovaModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.naver-clova.api-key:}") String apiKey,
            @Value("${app.ai.providers.naver-clova.gateway-key:}") String gatewayKey,
            @Value("${app.ai.providers.naver-clova.base-url:https://clovastudio.stream.ntruss.com}") String baseUrl,
            @Value("${app.ai.providers.naver-clova.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.naver-clova.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.naver-clova.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.naver-clova.supported-models:HCX-003,HCX-DASH-001}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderNaverClova"),
                new CompositeAuthStrategy(List.of(
                        new ApiKeyHeaderAuth("X-NCP-CLOVASTUDIO-API-KEY", apiKey),
                        new ApiKeyHeaderAuth("X-NCP-APIGW-API-KEY", gatewayKey)
                )),
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
    protected Object buildChatRequestBody(ChatRequest request, String model) {
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", request.prompt()));
        payload.put("messages", messages);
        if (request.temperature() != null) payload.put("temperature", request.temperature());
        if (request.maxTokens() != null) payload.put("maxTokens", request.maxTokens());
        else payload.put("maxTokens", 1024);
        return payload;
    }

    @Override
    protected ChatResult parseChatResponse(JsonNode responseRoot, String model) {
        JsonNode result = responseRoot.path("result");
        String content = result.path("message").path("content").asText("");
        Integer inputTokens = result.has("inputLength") ? result.path("inputLength").asInt(0) : null;
        Integer outputTokens = result.has("outputLength") ? result.path("outputLength").asInt(0) : null;
        String stopReason = result.path("stopReason").asText(null);

        return new ChatResult(content, model, providerName(), false, 1,
                inputTokens, outputTokens, stopReason);
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl + "/testapp/v1/chat-completions/" + model;
    }
}
