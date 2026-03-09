package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.application.ai.port.out.capability.RerankCapable;
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
public class CohereModelProvider implements MultiCapabilityProvider,
        ChatCapable, EmbeddingCapable, RerankCapable {

    private static final String PROVIDER_NAME = "cohere";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS, Capability.RERANK);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final String apiKey;
    private final String baseUrl;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final List<String> supportedModels;

    public CohereModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.cohere.api-key:}") String apiKey,
            @Value("${app.ai.providers.cohere.base-url:https://api.cohere.com}") String baseUrl,
            @Value("${app.ai.providers.cohere.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.cohere.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.cohere.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.cohere.supported-models:command-r,command-r-plus}") List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProviderCohere");
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
            throw new TechnicalException(ErrorCode.AI_002, "Cohere circuit breaker is open", ex);
        }
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        String model = request.model() != null ? request.model() : "embed-english-v3.0";
        Supplier<EmbeddingResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callEmbed(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Cohere circuit breaker is open", ex);
        }
    }

    @Override
    public RerankResult rerank(RerankRequest request) {
        String model = request.model() != null ? request.model() : "rerank-english-v3.0";
        Supplier<RerankResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callRerank(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Cohere circuit breaker is open", ex);
        }
    }

    // ─── Chat ─────────────────────────────────────────────

    private ChatResult callChat(ChatRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("message", request.prompt());
            if (request.systemPrompt() != null) body.put("preamble", request.systemPrompt());
            if (request.temperature() != null) body.put("temperature", request.temperature());
            if (request.maxTokens() != null) body.put("max_tokens", request.maxTokens());

            JsonNode root = sendJsonPost(baseUrl + "/v2/chat", body);

            // Cohere v2 response format
            JsonNode contentArray = root.path("message").path("content");
            String content = null;
            if (contentArray.isArray() && !contentArray.isEmpty()) {
                content = contentArray.get(0).path("text").asText(null);
            }
            if (content == null || content.isBlank()) {
                content = root.path("text").asText(null);
            }
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Cohere returned empty response content");
            }

            Integer inputTokens = root.path("usage").has("input_tokens")
                    ? root.path("usage").path("input_tokens").asInt() : null;
            Integer outputTokens = root.path("usage").has("output_tokens")
                    ? root.path("usage").path("output_tokens").asInt() : null;
            String finishReason = root.path("finish_reason").asText(null);

            return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                    inputTokens, outputTokens, finishReason);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Cohere chat request failed", ex);
        }
    }

    // ─── Embeddings ───────────────────────────────────────

    private EmbeddingResult callEmbed(EmbeddingRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("texts", request.input());
            body.put("input_type", "search_document");

            JsonNode root = sendJsonPost(baseUrl + "/v2/embed", body);

            List<float[]> embeddings = new ArrayList<>();
            JsonNode embeddingsNode = root.path("embeddings");
            int dimensions = 0;
            if (embeddingsNode.isArray()) {
                for (JsonNode vec : embeddingsNode) {
                    float[] vector = new float[vec.size()];
                    for (int i = 0; i < vec.size(); i++) {
                        vector[i] = (float) vec.get(i).asDouble();
                    }
                    embeddings.add(vector);
                    if (dimensions == 0) dimensions = vector.length;
                }
            }

            Integer totalTokens = root.path("meta").path("billed_units").has("input_tokens")
                    ? root.path("meta").path("billed_units").path("input_tokens").asInt() : null;

            return new EmbeddingResult(embeddings, model, PROVIDER_NAME, dimensions, totalTokens);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Cohere embedding request failed", ex);
        }
    }

    // ─── Rerank ───────────────────────────────────────────

    private RerankResult callRerank(RerankRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("query", request.query());

            List<String> docTexts = request.documents().stream()
                    .map(RerankDocument::text)
                    .toList();
            body.put("documents", docTexts);

            if (request.topN() != null) body.put("top_n", request.topN());

            JsonNode root = sendJsonPost(baseUrl + "/v2/rerank", body);

            List<RerankResult.ScoredDocument> scored = new ArrayList<>();
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode r : results) {
                    int index = r.path("index").asInt();
                    double score = r.path("relevance_score").asDouble();
                    RerankDocument doc = index < request.documents().size()
                            ? request.documents().get(index) : null;
                    scored.add(new RerankResult.ScoredDocument(index, score, doc));
                }
            }

            return new RerankResult(scored, model, PROVIDER_NAME);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Cohere rerank request failed", ex);
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
            throw new TechnicalException(ErrorCode.AI_005, "Failed to parse Cohere response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "Cohere request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Cohere request failed", ex);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429)
            throw new TechnicalException(ErrorCode.AI_003, "Cohere rate limit exceeded");
        if (response.statusCode() >= 500)
            throw new TechnicalException(ErrorCode.AI_002, "Cohere provider unavailable");
        if (response.statusCode() >= 400)
            throw new TechnicalException(ErrorCode.AI_007, "Cohere rejected request: " + response.statusCode());
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
                ? new TechnicalException(ErrorCode.AI_002, "Cohere request failed after retries")
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
            throw new TechnicalException(ErrorCode.AI_002, "Cohere retry interrupted", e);
        }
    }
}
