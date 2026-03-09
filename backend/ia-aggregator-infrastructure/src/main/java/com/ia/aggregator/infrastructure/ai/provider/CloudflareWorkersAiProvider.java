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
 * Cloudflare Workers AI provider.
 *
 * <p>API format: {@code https://api.cloudflare.com/client/v4/accounts/{accountId}/ai/run/{model}}
 * <p>Supports: CHAT, EMBEDDINGS (via separate adapter in future phases)
 * <p>Auth: Bearer token
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.cloudflare.api-key")
public class CloudflareWorkersAiProvider extends AbstractNativeLlmProvider {

    private static final String PROVIDER_NAME = "cloudflare";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    private final String accountId;

    public CloudflareWorkersAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.cloudflare.api-key:}") String apiKey,
            @Value("${app.ai.providers.cloudflare.account-id:}") String accountId,
            @Value("${app.ai.providers.cloudflare.base-url:https://api.cloudflare.com/client/v4}") String baseUrl,
            @Value("${app.ai.providers.cloudflare.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.cloudflare.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.cloudflare.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.cloudflare.supported-models:@cf/meta/llama-3.1-8b-instruct,@cf/mistral/mistral-7b-instruct-v0.2}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderCloudflare"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
        this.accountId = accountId;
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
        JsonNode result = root.path("result");
        String content = result.path("response").asText("");
        if (content.isBlank()) {
            content = result.path("choices").path(0).path("message").path("content").asText("");
        }
        return new ChatResult(content, model, PROVIDER_NAME, false, 1);
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl + "/accounts/" + accountId + "/ai/run/" + model;
    }
}
