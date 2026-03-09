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
 * Hugging Face Inference API provider.
 *
 * <p>API format: {@code https://api-inference.huggingface.co/models/{model}}
 * <p>Supports: CHAT
 * <p>Auth: Bearer token (HF token)
 *
 * <p>URL changes per model — overrides {@link #resolveEndpointUrl(String)}.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.huggingface.api-key")
public class HuggingFaceInferenceProvider extends AbstractNativeLlmProvider {

    private static final String PROVIDER_NAME = "huggingface";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    public HuggingFaceInferenceProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.huggingface.api-key:}") String apiKey,
            @Value("${app.ai.providers.huggingface.base-url:https://api-inference.huggingface.co}") String baseUrl,
            @Value("${app.ai.providers.huggingface.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.huggingface.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.huggingface.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.huggingface.supported-models:meta-llama/Llama-3.2-3B-Instruct,mistralai/Mistral-7B-Instruct-v0.3}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderHuggingface"),
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
        body.put("inputs", request.prompt());
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (request.maxTokens() != null) parameters.put("max_new_tokens", request.maxTokens());
        if (request.temperature() != null) parameters.put("temperature", request.temperature());
        if (!parameters.isEmpty()) body.put("parameters", parameters);
        return body;
    }

    @Override
    protected ChatResult parseChatResponse(JsonNode root, String model) {
        // HuggingFace returns array: [{"generated_text": "..."}]
        String content;
        if (root.isArray() && root.size() > 0) {
            content = root.get(0).path("generated_text").asText("");
        } else {
            content = root.path("generated_text").asText(root.asText(""));
        }
        return new ChatResult(content, model, PROVIDER_NAME, false, 1);
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl + "/models/" + model;
    }
}
