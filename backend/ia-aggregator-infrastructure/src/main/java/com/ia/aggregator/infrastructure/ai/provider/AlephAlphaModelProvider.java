package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.ChatRequest;
import com.ia.aggregator.application.ai.dto.ChatResult;
import com.ia.aggregator.application.ai.dto.EmbeddingRequest;
import com.ia.aggregator.application.ai.dto.EmbeddingResult;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractNativeLlmProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Aleph Alpha (Luminous) provider — European sovereign AI with native API format.
 *
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>API: {@code https://api.aleph-alpha.com}
 *
 * <p>Uses /complete for chat and /semantic_embed for embeddings.
 * Native format, not OpenAI-compatible.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.aleph-alpha.api-key")
public class AlephAlphaModelProvider extends AbstractNativeLlmProvider implements EmbeddingCapable {

    private static final String PROVIDER_NAME = "aleph-alpha";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    public AlephAlphaModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.aleph-alpha.api-key:}") String apiKey,
            @Value("${app.ai.providers.aleph-alpha.base-url:https://api.aleph-alpha.com}") String baseUrl,
            @Value("${app.ai.providers.aleph-alpha.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.aleph-alpha.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.aleph-alpha.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.aleph-alpha.supported-models:luminous-supreme-control,luminous-extended-control,luminous-base-control}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderAlephAlpha"),
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("prompt", request.prompt());
        if (request.maxTokens() != null) payload.put("maximum_tokens", request.maxTokens());
        else payload.put("maximum_tokens", 1024);
        if (request.temperature() != null) payload.put("temperature", request.temperature());
        return payload;
    }

    @Override
    protected ChatResult parseChatResponse(JsonNode responseRoot, String model) {
        JsonNode completions = responseRoot.path("completions");
        String content = "";
        if (completions.isArray() && !completions.isEmpty()) {
            content = completions.get(0).path("completion").asText("");
        }

        Integer promptTokens = responseRoot.has("num_tokens_prompt_total")
                ? responseRoot.path("num_tokens_prompt_total").asInt(0) : null;
        Integer completionTokens = responseRoot.has("num_tokens_generated")
                ? responseRoot.path("num_tokens_generated").asInt(0) : null;
        String finishReason = completions.isArray() && !completions.isEmpty()
                ? completions.get(0).path("finish_reason").asText(null) : null;

        return new ChatResult(content, model, providerName(), false, 1,
                promptTokens, completionTokens, finishReason);
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl + "/complete";
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        try {
            String model = request.model() != null ? request.model() : "luminous-base";
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("prompt", request.input());
            payload.put("representation", "symmetric");
            payload.put("compress_to_size", request.dimensions() != null ? request.dimensions() : 128);

            String json = objectMapper.writeValueAsString(payload);
            java.net.http.HttpRequest.Builder reqBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/semantic_embed"))
                    .timeout(java.time.Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json));
            authStrategy.apply(reqBuilder);

            java.net.http.HttpResponse<String> response = httpClient.send(
                    reqBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_008,
                        providerName() + " embedding failed HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddingNode = root.path("embedding");
            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }

            return new EmbeddingResult(List.of(vector), model, providerName(),
                    vector.length, root.has("num_tokens_prompt_total")
                            ? root.path("num_tokens_prompt_total").asInt(0) : null);
        } catch (TechnicalException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalException(ErrorCode.AI_008,
                    providerName() + " embedding request failed: " + e.getMessage());
        }
    }
}
