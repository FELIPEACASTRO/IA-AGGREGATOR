package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
import com.ia.aggregator.application.ai.port.out.capability.ResponsesCapable;
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
public class AnthropicModelProvider implements MultiCapabilityProvider,
        ChatCapable, ResponsesCapable {

    private static final String PROVIDER_NAME = "anthropic";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.RESPONSES);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final String apiKey;
    private final String baseUrl;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final int maxTokens;
    private final List<String> supportedModels;

    public AnthropicModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.anthropic.api-key:}") String apiKey,
            @Value("${app.ai.providers.anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${app.ai.providers.anthropic.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.anthropic.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.anthropic.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.anthropic.max-tokens:800}") int maxTokens,
            @Value("${app.ai.providers.anthropic.supported-models:claude-3-5-haiku}") List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProviderAnthropic");
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.maxTokens = maxTokens;
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
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic circuit breaker is open", ex);
        }
    }

    @Override
    public ResponsesResult responses(ResponsesRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);
        Supplier<ResponsesResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callResponses(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic circuit breaker is open", ex);
        }
    }

    // ─── Chat ─────────────────────────────────────────────

    private ChatResult callChat(ChatRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            int tokens = request.maxTokens() != null ? request.maxTokens() : maxTokens;
            body.put("max_tokens", tokens);

            if (request.systemPrompt() != null) {
                body.put("system", request.systemPrompt());
            }

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", request.prompt())
            );
            body.put("messages", messages);
            if (request.temperature() != null) body.put("temperature", request.temperature());

            JsonNode root = sendAnthropicPost(baseUrl + "/v1/messages", body);

            String content = root.path("content").path(0).path("text").asText(null);
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Anthropic returned empty response content");
            }
            Integer inputTokens = root.path("usage").has("input_tokens")
                    ? root.path("usage").path("input_tokens").asInt() : null;
            Integer outputTokens = root.path("usage").has("output_tokens")
                    ? root.path("usage").path("output_tokens").asInt() : null;
            String stopReason = root.path("stop_reason").asText(null);

            return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                    inputTokens, outputTokens, stopReason);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic chat request failed", ex);
        }
    }

    // ─── Responses (Anthropic Messages API with tool_use) ─

    private ResponsesResult callResponses(ResponsesRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            int tokens = request.maxOutputTokens() != null ? request.maxOutputTokens() : maxTokens;
            body.put("max_tokens", tokens);

            if (request.instructions() != null) {
                body.put("system", request.instructions());
            }
            body.put("messages", List.of(Map.of("role", "user", "content", request.input())));

            if (request.tools() != null && !request.tools().isEmpty()) {
                body.put("tools", request.tools());
            }
            if (request.temperature() != null) body.put("temperature", request.temperature());

            JsonNode root = sendAnthropicPost(baseUrl + "/v1/messages", body);

            // Parse content blocks as output items
            List<Map<String, Object>> outputItems = new ArrayList<>();
            JsonNode contentArray = root.path("content");
            if (contentArray.isArray()) {
                for (JsonNode block : contentArray) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("type", block.path("type").asText());
                    if ("text".equals(block.path("type").asText())) {
                        item.put("text", block.path("text").asText());
                    } else if ("tool_use".equals(block.path("type").asText())) {
                        item.put("id", block.path("id").asText());
                        item.put("name", block.path("name").asText());
                        item.put("input", objectMapper.convertValue(block.path("input"), Map.class));
                    }
                    outputItems.add(item);
                }
            }

            Integer inputTokens = root.path("usage").has("input_tokens")
                    ? root.path("usage").path("input_tokens").asInt() : null;
            Integer outputTokens = root.path("usage").has("output_tokens")
                    ? root.path("usage").path("output_tokens").asInt() : null;

            return new ResponsesResult(outputItems, model, PROVIDER_NAME, inputTokens, outputTokens);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic responses request failed", ex);
        }
    }

    // ─── HTTP infrastructure ──────────────────────────────

    private JsonNode sendAnthropicPost(String endpoint, Map<String, Object> body) {
        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);
            return objectMapper.readTree(response.body());
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_005, "Failed to parse Anthropic response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic request failed", ex);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429)
            throw new TechnicalException(ErrorCode.AI_003, "Anthropic rate limit exceeded");
        if (response.statusCode() >= 500)
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic provider unavailable");
        if (response.statusCode() >= 400)
            throw new TechnicalException(ErrorCode.AI_007, "Anthropic rejected request: " + response.statusCode());
    }

    @SuppressWarnings("unchecked")
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
                ? new TechnicalException(ErrorCode.AI_002, "Anthropic request failed after retries")
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
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic retry interrupted", e);
        }
    }
}
