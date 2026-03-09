package com.ia.aggregator.infrastructure.ai.provider.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.ChatRequest;
import com.ia.aggregator.application.ai.dto.ChatResult;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.infrastructure.ai.auth.AuthStrategy;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract base for providers with native (non-OpenAI) API formats.
 * Each subclass implements custom request building and response parsing.
 *
 * <p>Design Pattern: Template Method — subclasses override:
 * <ul>
 *   <li>{@link #buildChatRequestBody(ChatRequest, String)} — custom JSON body</li>
 *   <li>{@link #parseChatResponse(JsonNode, String)} — custom response extraction</li>
 *   <li>{@link #resolveEndpointUrl(String)} — custom URL construction</li>
 * </ul>
 */
public abstract class AbstractNativeLlmProvider implements MultiCapabilityProvider, ChatCapable {

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final CircuitBreaker circuitBreaker;
    protected final AuthStrategy authStrategy;
    protected final String baseUrl;
    protected final long timeoutMs;
    protected final int retryAttempts;
    protected final long retryBackoffMs;
    protected final List<String> supportedModels;

    protected AbstractNativeLlmProvider(
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

    // ---------- Abstract template methods ----------

    /** Builds the provider-specific JSON request body for chat. */
    protected abstract Object buildChatRequestBody(ChatRequest request, String model);

    /** Parses the provider-specific chat response. */
    protected abstract ChatResult parseChatResponse(JsonNode responseRoot, String model);

    /** Resolves the full endpoint URL for the given model/operation. */
    protected abstract String resolveEndpointUrl(String model);

    // ---------- Core execution ----------

    private ChatResult executeChatCall(ChatRequest request, String model) {
        try {
            Object payload = buildChatRequestBody(request, model);
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(resolveEndpointUrl(model)))
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

            JsonNode root = objectMapper.readTree(response.body());
            return parseChatResponse(root, model);
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
