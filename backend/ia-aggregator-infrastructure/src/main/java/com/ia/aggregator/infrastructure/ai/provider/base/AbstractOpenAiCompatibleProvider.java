package com.ia.aggregator.infrastructure.ai.provider.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.AuthStrategy;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

/**
 * Abstract base for providers compatible with the OpenAI API format.
 * Eliminates code duplication for retry, backoff, circuit breaker, and JSON parsing.
 *
 * <p>Design Pattern: Template Method — subclasses only need to provide:
 * <ul>
 *   <li>{@link #providerName()} — unique provider identifier</li>
 *   <li>{@link #capabilities()} — set of supported capabilities</li>
 *   <li>Configuration values (injected via constructor)</li>
 * </ul>
 *
 * <p>Big O: O(R * P) where R = retry attempts, P = providers in fallback chain.
 * Each provider call is O(1) amortized (HTTP round-trip dominated).
 */
public abstract class AbstractOpenAiCompatibleProvider
        implements MultiCapabilityProvider, ChatCapable, EmbeddingCapable {

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final CircuitBreaker circuitBreaker;
    protected final AuthStrategy authStrategy;
    protected final String baseUrl;
    protected final long timeoutMs;
    protected final int retryAttempts;
    protected final long retryBackoffMs;
    protected final List<String> supportedModels;

    protected AbstractOpenAiCompatibleProvider(
            ObjectMapper objectMapper,
            CircuitBreaker circuitBreaker,
            AuthStrategy authStrategy,
            String baseUrl,
            long timeoutMs,
            int retryAttempts,
            long retryBackoffMs,
            List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.authStrategy = authStrategy;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.supportedModels = supportedModels;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public boolean supports(String model) {
        return supportedModels.stream().map(String::trim).anyMatch(model::equals);
    }

    @Override
    public String generate(String prompt, String model) {
        ChatResult result = chat(new ChatRequest(prompt, model));
        return result.content();
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);
        Supplier<ChatResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> executeChatCall(request, model));

        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002,
                    providerName() + " circuit breaker is open", ex);
        }
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        if (!supportsCapability(Capability.EMBEDDINGS)) {
            throw new TechnicalException(ErrorCode.AI_008,
                    providerName() + " does not support embeddings");
        }

        String model = request.model() != null ? request.model() : resolveDefaultEmbeddingModel();
        Supplier<EmbeddingResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> executeEmbeddingCall(request, model));

        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002,
                    providerName() + " circuit breaker is open", ex);
        }
    }

    // ---------- Template methods for subclass customization ----------

    /**
     * Returns the default embedding model for this provider.
     * Override if the provider uses a different default model for embeddings.
     */
    protected String resolveDefaultEmbeddingModel() {
        return supportedModels.isEmpty() ? "text-embedding-3-small" : supportedModels.get(0);
    }

    /**
     * Returns the chat completions endpoint path.
     * Override if the provider uses a different path.
     */
    protected String chatEndpointPath() {
        return "/v1/chat/completions";
    }

    /**
     * Returns the embeddings endpoint path.
     */
    protected String embeddingEndpointPath() {
        return "/v1/embeddings";
    }

    // ---------- Core execution (shared logic) ----------

    private ChatResult executeChatCall(ChatRequest request, String model) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("messages", List.of(Map.of("role", "user", "content", request.prompt())));
            if (request.temperature() != null) payload.put("temperature", request.temperature());
            if (request.maxTokens() != null) payload.put("max_tokens", request.maxTokens());

            String responseBody = sendJsonRequest(chatEndpointPath(), payload);
            JsonNode root = objectMapper.readTree(responseBody);

            handleErrorResponse(root);

            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005,
                        providerName() + " returned empty response content");
            }

            Integer promptTokens = root.has("usage") ? root.path("usage").path("prompt_tokens").asInt(0) : null;
            Integer completionTokens = root.has("usage") ? root.path("usage").path("completion_tokens").asInt(0) : null;
            String finishReason = root.path("choices").path(0).path("finish_reason").asText(null);

            return new ChatResult(content.asText(), model, providerName(), false, 1,
                    promptTokens, completionTokens, finishReason);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002,
                    providerName() + " chat request failed", ex);
        }
    }

    private EmbeddingResult executeEmbeddingCall(EmbeddingRequest request, String model) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("input", request.input());
            if (request.dimensions() != null) payload.put("dimensions", request.dimensions());

            String responseBody = sendJsonRequest(embeddingEndpointPath(), payload);
            JsonNode root = objectMapper.readTree(responseBody);

            handleErrorResponse(root);

            List<float[]> embeddings = new ArrayList<>();
            JsonNode dataArray = root.path("data");
            for (JsonNode item : dataArray) {
                JsonNode embeddingNode = item.path("embedding");
                float[] vector = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    vector[i] = (float) embeddingNode.get(i).asDouble();
                }
                embeddings.add(vector);
            }

            int dimensions = embeddings.isEmpty() ? 0 : embeddings.get(0).length;
            Integer totalTokens = root.has("usage") ? root.path("usage").path("total_tokens").asInt(0) : null;

            return new EmbeddingResult(embeddings, model, providerName(), dimensions, totalTokens);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_008,
                    providerName() + " embedding request failed", ex);
        }
    }

    protected String sendJsonRequest(String path, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            authStrategy.apply(requestBuilder);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                throw new TechnicalException(ErrorCode.AI_003,
                        providerName() + " rate limit exceeded");
            }
            if (response.statusCode() >= 500) {
                throw new TechnicalException(ErrorCode.AI_002,
                        providerName() + " server error: " + response.statusCode());
            }
            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_007,
                        providerName() + " rejected request with status " + response.statusCode());
            }

            return response.body();
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_005,
                    "Failed to communicate with " + providerName(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002,
                    providerName() + " request interrupted", ex);
        }
    }

    protected void handleErrorResponse(JsonNode root) {
        if (root.has("error")) {
            String errorMessage = root.path("error").path("message").asText("Unknown error");
            throw new TechnicalException(ErrorCode.AI_007,
                    providerName() + " API error: " + errorMessage);
        }
    }

    protected <T> T executeWithRetry(Supplier<T> guardedCall) {
        TechnicalException lastError = null;
        int maxAttempts = Math.max(1, retryAttempts);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return guardedCall.get();
            } catch (TechnicalException ex) {
                lastError = ex;
                if (!shouldRetry(ex) || attempt == maxAttempts) {
                    throw ex;
                }
                sleepBackoff();
            }
        }

        throw lastError == null
                ? new TechnicalException(ErrorCode.AI_002, providerName() + " request failed after retries")
                : lastError;
    }

    private boolean shouldRetry(TechnicalException ex) {
        return ex.getErrorCode() == ErrorCode.AI_002
                || ex.getErrorCode() == ErrorCode.AI_003
                || ex.getErrorCode() == ErrorCode.AI_005;
    }

    private void sleepBackoff() {
        if (retryBackoffMs <= 0) return;
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002,
                    providerName() + " retry interrupted", ex);
        }
    }
}
