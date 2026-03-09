package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.ChatRequest;
import com.ia.aggregator.application.ai.dto.ChatResult;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractNativeLlmProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI21 Labs provider (Jamba models).
 *
 * <p>API: {@code https://api.ai21.com/studio/v1/chat/completions}
 * <p>Supports: CHAT
 * <p>Auth: Bearer token
 *
 * <p>AI21 uses a format similar to OpenAI but with proprietary extensions (Jamba).
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.ai21.api-key")
public class Ai21LabsProvider extends AbstractNativeLlmProvider {

    private static final String PROVIDER_NAME = "ai21";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public Ai21LabsProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.ai21.api-key:}") String apiKey,
            @Value("${app.ai.providers.ai21.base-url:https://api.ai21.com/studio/v1}") String baseUrl,
            @Value("${app.ai.providers.ai21.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.ai21.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.ai21.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.ai21.supported-models:jamba-1.5-mini,jamba-1.5-large}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderAi21"),
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
    protected Object buildChatRequestBody(ChatRequest request, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.systemPrompt() != null) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.prompt()));
        body.put("messages", messages);
        if (request.maxTokens() != null) body.put("max_tokens", request.maxTokens());
        if (request.temperature() != null) body.put("temperature", request.temperature());
        return body;
    }

    @Override
    protected ChatResult parseChatResponse(JsonNode root, String model) {
        // AI21 follows OpenAI format for Jamba chat completions
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        Integer promptTokens = root.has("usage") ? root.path("usage").path("prompt_tokens").asInt(0) : null;
        Integer completionTokens = root.has("usage") ? root.path("usage").path("completion_tokens").asInt(0) : null;
        String finishReason = root.path("choices").path(0).path("finish_reason").asText(null);
        return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                promptTokens, completionTokens, finishReason);
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl + "/chat/completions";
    }
}
