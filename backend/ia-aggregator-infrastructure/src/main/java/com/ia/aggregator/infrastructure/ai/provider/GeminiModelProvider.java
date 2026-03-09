package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.application.ai.port.out.capability.WebGroundedChatCapable;
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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Component
public class GeminiModelProvider implements MultiCapabilityProvider,
        ChatCapable, EmbeddingCapable, WebGroundedChatCapable {

    private static final String PROVIDER_NAME = "gemini";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS, Capability.WEB_GROUNDED_CHAT);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final String apiKey;
    private final String baseUrl;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final List<String> supportedModels;

    public GeminiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.gemini.api-key:}") String apiKey,
            @Value("${app.ai.providers.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.ai.providers.gemini.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.gemini.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.gemini.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.gemini.supported-models:gemini-1.5-flash}") List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProviderGemini");
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
            throw new TechnicalException(ErrorCode.AI_002, "Gemini circuit breaker is open", ex);
        }
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        String model = request.model() != null ? request.model() : "text-embedding-004";
        Supplier<EmbeddingResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callEmbed(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Gemini circuit breaker is open", ex);
        }
    }

    @Override
    public GroundedChatResult groundedChat(GroundedChatRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);
        Supplier<GroundedChatResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callGroundedChat(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Gemini circuit breaker is open", ex);
        }
    }

    // ─── Chat ─────────────────────────────────────────────

    private ChatResult callChat(ChatRequest request, String model) {
        try {
            Map<String, Object> body = buildGeminiBody(request.prompt(), request.systemPrompt());
            if (request.temperature() != null) {
                body.put("generationConfig", Map.of("temperature", request.temperature()));
            }

            String endpoint = geminiEndpoint(model, "generateContent");
            JsonNode root = sendGeminiPost(endpoint, body);

            String content = root.path("candidates").path(0).path("content")
                    .path("parts").path(0).path("text").asText(null);
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Gemini returned empty response content");
            }

            Integer totalTokens = root.path("usageMetadata").has("totalTokenCount")
                    ? root.path("usageMetadata").path("totalTokenCount").asInt() : null;

            return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                    null, null, "stop");
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Gemini chat request failed", ex);
        }
    }

    // ─── Embeddings ───────────────────────────────────────

    private EmbeddingResult callEmbed(EmbeddingRequest request, String model) {
        try {
            // Gemini uses batchEmbedContents for multiple texts
            List<Map<String, Object>> requests = new ArrayList<>();
            for (String text : request.input()) {
                Map<String, Object> req = new LinkedHashMap<>();
                req.put("model", "models/" + model);
                req.put("content", Map.of("parts", List.of(Map.of("text", text))));
                if (request.dimensions() != null) {
                    req.put("outputDimensionality", request.dimensions());
                }
                requests.add(req);
            }

            Map<String, Object> body = Map.of("requests", requests);
            String endpoint = geminiEndpoint(model, "batchEmbedContents");
            JsonNode root = sendGeminiPost(endpoint, body);

            List<float[]> embeddings = new ArrayList<>();
            int dimensions = 0;
            JsonNode embeddingsNode = root.path("embeddings");
            if (embeddingsNode.isArray()) {
                for (JsonNode emb : embeddingsNode) {
                    JsonNode values = emb.path("values");
                    float[] vector = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        vector[i] = (float) values.get(i).asDouble();
                    }
                    embeddings.add(vector);
                    if (dimensions == 0) dimensions = vector.length;
                }
            }

            return new EmbeddingResult(embeddings, model, PROVIDER_NAME, dimensions, null);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Gemini embedding request failed", ex);
        }
    }

    // ─── Grounded Chat (Google Search grounding) ──────────

    private GroundedChatResult callGroundedChat(GroundedChatRequest request, String model) {
        try {
            Map<String, Object> body = buildGeminiBody(request.query(), request.systemPrompt());

            // Enable Google Search grounding
            body.put("tools", List.of(Map.of("google_search_retrieval", Map.of())));

            if (request.temperature() != null) {
                body.put("generationConfig", Map.of("temperature", request.temperature()));
            }

            String endpoint = geminiEndpoint(model, "generateContent");
            JsonNode root = sendGeminiPost(endpoint, body);

            String content = root.path("candidates").path(0).path("content")
                    .path("parts").path(0).path("text").asText(null);
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Gemini grounded chat returned empty");
            }

            // Parse grounding metadata citations
            List<GroundedChatResult.Citation> citations = new ArrayList<>();
            JsonNode groundingMeta = root.path("candidates").path(0).path("groundingMetadata");
            JsonNode groundingChunks = groundingMeta.path("groundingChunks");
            if (groundingChunks.isArray()) {
                for (JsonNode chunk : groundingChunks) {
                    JsonNode web = chunk.path("web");
                    citations.add(new GroundedChatResult.Citation(
                            web.path("title").asText(null),
                            web.path("uri").asText(null),
                            null
                    ));
                }
            }

            return new GroundedChatResult(content, citations, model, PROVIDER_NAME);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Gemini grounded chat request failed", ex);
        }
    }

    // ─── Helpers ──────────────────────────────────────────

    private Map<String, Object> buildGeminiBody(String prompt, String systemPrompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (systemPrompt != null) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }
        body.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));
        return body;
    }

    private String geminiEndpoint(String model, String method) {
        String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        return String.format("%s/v1beta/models/%s:%s?key=%s", baseUrl, model, method, encodedKey);
    }

    // ─── HTTP infrastructure ──────────────────────────────

    private JsonNode sendGeminiPost(String endpoint, Map<String, Object> body) {
        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);
            return objectMapper.readTree(response.body());
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_005, "Failed to parse Gemini response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "Gemini request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Gemini request failed", ex);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429)
            throw new TechnicalException(ErrorCode.AI_003, "Gemini rate limit exceeded");
        if (response.statusCode() >= 500)
            throw new TechnicalException(ErrorCode.AI_002, "Gemini provider unavailable");
        if (response.statusCode() >= 400)
            throw new TechnicalException(ErrorCode.AI_007, "Gemini rejected request: " + response.statusCode());
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
                ? new TechnicalException(ErrorCode.AI_002, "Gemini request failed after retries")
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
            throw new TechnicalException(ErrorCode.AI_002, "Gemini retry interrupted", e);
        }
    }
}
