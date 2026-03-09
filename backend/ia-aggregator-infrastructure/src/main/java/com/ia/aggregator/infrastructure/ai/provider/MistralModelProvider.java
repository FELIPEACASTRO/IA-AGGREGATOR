package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Component
public class MistralModelProvider implements MultiCapabilityProvider,
        ChatCapable, EmbeddingCapable {

    private static final String PROVIDER_NAME = "mistral";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final String apiKey;
    private final String baseUrl;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final List<String> supportedModels;

    public MistralModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.mistral.api-key:}") String apiKey,
            @Value("${app.ai.providers.mistral.base-url:https://api.mistral.ai}") String baseUrl,
            @Value("${app.ai.providers.mistral.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.mistral.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.mistral.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.mistral.supported-models:mistral-small-latest,mistral-large-latest}") List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProviderMistral");
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.supportedModels = supportedModels;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public boolean supports(String model) {
        return apiKey != null
                && !apiKey.isBlank()
                && supportedModels.stream().map(String::trim).anyMatch(model::equals);
    }

    @Override
    public String generate(String prompt, String model) {
        return chat(new ChatRequest(prompt, model)).content();
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);
        Supplier<ChatResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callChat(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Mistral circuit breaker is open", ex);
        }
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        String model = request.model() != null ? request.model() : "mistral-embed";
        Supplier<EmbeddingResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callEmbed(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Mistral circuit breaker is open", ex);
        }
    }

    // ─── Chat ─────────────────────────────────────────────

    private ChatResult callChat(ChatRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            List<Map<String, String>> messages = new ArrayList<>();
            if (request.systemPrompt() != null) {
                messages.add(Map.of("role", "system", "content", request.systemPrompt()));
            }
            messages.add(Map.of("role", "user", "content", request.prompt()));
            body.put("messages", messages);
            if (request.temperature() != null) body.put("temperature", request.temperature());
            if (request.maxTokens() != null) body.put("max_tokens", request.maxTokens());

            JsonNode root = sendJsonPost(baseUrl + "/v1/chat/completions", body);

            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Mistral returned empty response content");
            }
            Integer promptTokens = root.path("usage").has("prompt_tokens")
                    ? root.path("usage").path("prompt_tokens").asInt() : null;
            Integer completionTokens = root.path("usage").has("completion_tokens")
                    ? root.path("usage").path("completion_tokens").asInt() : null;
            String finishReason = root.path("choices").path(0).path("finish_reason").asText(null);

            return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                    promptTokens, completionTokens, finishReason);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Mistral chat request failed", ex);
        }
    }

    // ─── Embeddings ───────────────────────────────────────

    private EmbeddingResult callEmbed(EmbeddingRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", request.input());

            JsonNode root = sendJsonPost(baseUrl + "/v1/embeddings", body);

            List<float[]> embeddings = new ArrayList<>();
            JsonNode data = root.path("data");
            int dimensions = 0;
            if (data.isArray()) {
                for (JsonNode item : data) {
                    JsonNode embedding = item.path("embedding");
                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = (float) embedding.get(i).asDouble();
                    }
                    embeddings.add(vector);
                    if (dimensions == 0) dimensions = vector.length;
                }
            }
            Integer totalTokens = root.path("usage").has("total_tokens")
                    ? root.path("usage").path("total_tokens").asInt() : null;

            return new EmbeddingResult(embeddings, model, PROVIDER_NAME, dimensions, totalTokens);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Mistral embedding request failed", ex);
        }
    }

    // ─── HTTP infrastructure ──────────────────────────────

    private JsonNode sendJsonPost(String endpoint, Map<String, Object> body) {
        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);
            return objectMapper.readTree(response.body());
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_005, "Failed to parse Mistral response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "Mistral request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Mistral request failed", ex);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429)
            throw new TechnicalException(ErrorCode.AI_003, "Mistral rate limit exceeded");
        if (response.statusCode() >= 500)
            throw new TechnicalException(ErrorCode.AI_002, "Mistral provider unavailable");
        if (response.statusCode() >= 400)
            throw new TechnicalException(ErrorCode.AI_007, "Mistral rejected request: " + response.statusCode());
    }

    private <T> T executeWithRetry(Supplier<T> guardedCall) {
        TechnicalException lastError = null;
        int maxAttempts = Math.max(1, retryAttempts);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return guardedCall.get();
            } catch (TechnicalException ex) {
                lastError = ex;
                if (!shouldRetry(ex) || attempt == maxAttempts) throw ex;
                sleepBackoff();
            }
        }
        throw lastError == null
                ? new TechnicalException(ErrorCode.AI_002, "Mistral request failed after retries")
                : lastError;
    }

    private boolean shouldRetry(TechnicalException ex) {
        return ex.getErrorCode() == ErrorCode.AI_002
                || ex.getErrorCode() == ErrorCode.AI_003
                || ex.getErrorCode() == ErrorCode.AI_005;
    }

    private void sleepBackoff() {
        if (retryBackoffMs <= 0) return;
        try { Thread.sleep(retryBackoffMs); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "Mistral retry interrupted", e);
        }
    }
}
