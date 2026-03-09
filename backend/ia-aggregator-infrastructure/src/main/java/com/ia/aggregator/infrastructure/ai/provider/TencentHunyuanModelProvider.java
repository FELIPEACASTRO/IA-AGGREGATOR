package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.ChatRequest;
import com.ia.aggregator.application.ai.dto.ChatResult;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.TencentHmacAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractNativeLlmProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tencent Hunyuan provider — native Tencent Cloud LLM API.
 *
 * <p>Supports: CHAT
 * <p>API: {@code https://hunyuan.tencentcloudapi.com}
 *
 * <p>Auth: TC3-HMAC-SHA256 request signing using SecretId + SecretKey.
 * Request format is Tencent-native (Action: ChatCompletions).
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.tencent-hunyuan.secret-id")
public class TencentHunyuanModelProvider extends AbstractNativeLlmProvider {

    private static final String PROVIDER_NAME = "tencent-hunyuan";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public TencentHunyuanModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.tencent-hunyuan.secret-id:}") String secretId,
            @Value("${app.ai.providers.tencent-hunyuan.secret-key:}") String secretKey,
            @Value("${app.ai.providers.tencent-hunyuan.base-url:https://hunyuan.tencentcloudapi.com}") String baseUrl,
            @Value("${app.ai.providers.tencent-hunyuan.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.tencent-hunyuan.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.tencent-hunyuan.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.tencent-hunyuan.supported-models:hunyuan-pro,hunyuan-standard,hunyuan-lite}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderTencentHunyuan"),
                new TencentHmacAuth(secretId, secretKey),
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
        payload.put("Model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("Role", "user", "Content", request.prompt()));
        payload.put("Messages", messages);
        if (request.temperature() != null) payload.put("Temperature", request.temperature());
        return payload;
    }

    @Override
    protected ChatResult parseChatResponse(JsonNode responseRoot, String model) {
        JsonNode response = responseRoot.path("Response");
        JsonNode choices = response.path("Choices");
        String content = "";
        String finishReason = null;
        if (choices.isArray() && !choices.isEmpty()) {
            content = choices.get(0).path("Message").path("Content").asText("");
            finishReason = choices.get(0).path("FinishReason").asText(null);
        }

        JsonNode usage = response.path("Usage");
        Integer promptTokens = usage.has("PromptTokens") ? usage.path("PromptTokens").asInt(0) : null;
        Integer completionTokens = usage.has("CompletionTokens") ? usage.path("CompletionTokens").asInt(0) : null;

        return new ChatResult(content, model, providerName(), false, 1,
                promptTokens, completionTokens, finishReason);
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl;
    }
}
