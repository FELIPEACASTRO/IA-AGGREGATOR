package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Component
public class PerplexityModelProvider implements MultiCapabilityProvider,
        ChatCapable, WebGroundedChatCapable {

    private static final String PROVIDER_NAME = "perplexity";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.WEB_GROUNDED_CHAT);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final String apiKey;
    private final String baseUrl;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final List<String> supportedModels;

    public PerplexityModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.perplexity.api-key:}") String apiKey,
            @Value("${app.ai.providers.perplexity.base-url:https://api.perplexity.ai}") String baseUrl,
            @Value("${app.ai.providers.perplexity.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.perplexity.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.perplexity.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.perplexity.supported-models:sonar,sonar-pro}") List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProviderPerplexity");
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
            throw new TechnicalException(ErrorCode.AI_002, "Perplexity circuit breaker is open", ex);
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
            throw new TechnicalException(ErrorCode.AI_002, "Perplexity circuit breaker is open", ex);
        }
    }

    // ─── Chat ─────────────────────────────────────────────

    private ChatResult callChat(ChatRequest request, String model) {
        try {
            Map<String, Object> body = buildChatBody(request.prompt(), request.systemPrompt(),
                    model, request.temperature(), request.maxTokens());
            JsonNode root = sendJsonPost(baseUrl + "/chat/completions", body);

            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Perplexity returned empty response content");
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
            throw new TechnicalException(ErrorCode.AI_002, "Perplexity chat request failed", ex);
        }
    }

    // ─── Grounded Chat (Perplexity returns citations) ─────

    private GroundedChatResult callGroundedChat(GroundedChatRequest request, String model) {
        try {
            Map<String, Object> body = buildChatBody(request.query(), request.systemPrompt(),
                    model, request.temperature(), null);
            JsonNode root = sendJsonPost(baseUrl + "/chat/completions", body);

            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Perplexity returned empty grounded chat");
            }

            // Parse Perplexity citations array
            List<GroundedChatResult.Citation> citations = new ArrayList<>();
            JsonNode citationsNode = root.path("citations");
            if (citationsNode.isArray()) {
                for (JsonNode c : citationsNode) {
                    if (c.isTextual()) {
                        citations.add(new GroundedChatResult.Citation(null, c.asText(), null));
                    } else {
                        citations.add(new GroundedChatResult.Citation(
                                c.path("title").asText(null),
                                c.path("url").asText(null),
                                c.path("snippet").asText(null)
                        ));
                    }
                }
            }

            return new GroundedChatResult(content, citations, model, PROVIDER_NAME);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Perplexity grounded chat request failed", ex);
        }
    }

    private Map<String, Object> buildChatBody(String prompt, String systemPrompt,
                                               String model, Double temperature, Integer maxTokens) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", prompt));
        body.put("messages", messages);
        if (temperature != null) body.put("temperature", temperature);
        if (maxTokens != null) body.put("max_tokens", maxTokens);
        return body;
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
            throw new TechnicalException(ErrorCode.AI_005, "Failed to parse Perplexity response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "Perplexity request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Perplexity request failed", ex);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429)
            throw new TechnicalException(ErrorCode.AI_003, "Perplexity rate limit exceeded");
        if (response.statusCode() >= 500)
            throw new TechnicalException(ErrorCode.AI_002, "Perplexity provider unavailable");
        if (response.statusCode() >= 400)
            throw new TechnicalException(ErrorCode.AI_007, "Perplexity rejected request: " + response.statusCode());
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
                ? new TechnicalException(ErrorCode.AI_002, "Perplexity request failed after retries")
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
            throw new TechnicalException(ErrorCode.AI_002, "Perplexity retry interrupted", e);
        }
    }
}
