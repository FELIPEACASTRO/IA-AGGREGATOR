package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.port.out.AiModelProvider;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
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
import java.util.List;
import java.util.function.Supplier;

@Component
public class AnthropicModelProvider implements AiModelProvider {

    private static final String PROVIDER_NAME = "anthropic";

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

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(String model) {
        return apiKey != null
                && !apiKey.isBlank()
                && supportedModels.stream().map(String::trim).anyMatch(model::equals);
    }

    @Override
    public String generate(String prompt, String model) {
        if (!supports(model)) {
            throw new TechnicalException(ErrorCode.AI_007, "Anthropic provider is not configured for model: " + model);
        }

        Supplier<String> guardedCall = CircuitBreaker.decorateSupplier(circuitBreaker, () -> callAnthropic(prompt, model));

        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic circuit breaker is open", ex);
        }
    }

    private String executeWithRetry(Supplier<String> guardedCall) {
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
                ? new TechnicalException(ErrorCode.AI_002, "Anthropic request failed after retries")
                : lastError;
    }

    private boolean shouldRetry(TechnicalException ex) {
        return ex.getErrorCode() == ErrorCode.AI_002
                || ex.getErrorCode() == ErrorCode.AI_003
                || ex.getErrorCode() == ErrorCode.AI_005;
    }

    private void sleepBackoff() {
        if (retryBackoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "Anthropic retry interrupted", interruptedException);
        }
    }

    private String callAnthropic(String prompt, String model) {
        try {
            String payload = objectMapper.writeValueAsString(new AnthropicRequest(model, maxTokens, prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/messages"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                throw new TechnicalException(ErrorCode.AI_003, "Anthropic rate limit exceeded");
            }
            if (response.statusCode() >= 500) {
                throw new TechnicalException(ErrorCode.AI_002, "Anthropic provider unavailable");
            }
            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_007, "Anthropic rejected request with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("content").path(0).path("text");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Anthropic returned empty response content");
            }

            return content.asText();
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

    private record AnthropicRequest(String model, int max_tokens, List<AnthropicMessage> messages) {
        private AnthropicRequest(String model, int maxTokens, String prompt) {
            this(model, maxTokens, List.of(new AnthropicMessage("user", prompt)));
        }
    }

    private record AnthropicMessage(String role, String content) {
    }
}