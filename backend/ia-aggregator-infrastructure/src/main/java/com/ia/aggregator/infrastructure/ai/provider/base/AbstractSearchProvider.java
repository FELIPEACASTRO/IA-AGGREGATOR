package com.ia.aggregator.infrastructure.ai.provider.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.infrastructure.ai.auth.AuthStrategy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract base for search and intelligence providers.
 *
 * <p>Design Pattern: Template Method — subclasses implement:
 * <ul>
 *   <li>{@link #buildSearchPayload} — build the request body for the search API</li>
 *   <li>{@link #parseSearchResponse} — parse the response into domain DTOs</li>
 * </ul>
 *
 * <p>Handles: JSON POST, error handling, retry, auth strategy.
 * <p>Big O: O(1) per request — all search APIs are synchronous.
 */
public abstract class AbstractSearchProvider implements MultiCapabilityProvider {

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final AuthStrategy authStrategy;
    protected final String baseUrl;
    protected final long timeoutMs;
    protected final int retryAttempts;
    protected final long retryBackoffMs;
    protected final List<String> supportedModels;

    protected AbstractSearchProvider(
            ObjectMapper objectMapper,
            AuthStrategy authStrategy,
            String baseUrl,
            long timeoutMs,
            int retryAttempts,
            long retryBackoffMs,
            List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
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
        throw new TechnicalException(ErrorCode.AI_007,
                providerName() + " does not support text generation. Use specific search methods.");
    }

    /**
     * Sends a JSON POST request and returns parsed response.
     */
    protected JsonNode sendJsonPost(String endpoint, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            authStrategy.apply(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                throw new TechnicalException(ErrorCode.AI_003, providerName() + " rate limit exceeded");
            }
            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_002,
                        providerName() + " request failed (" + response.statusCode() + "): "
                                + truncate(response.body(), 200));
            }

            return objectMapper.readTree(response.body());
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_002, providerName() + " I/O error", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, providerName() + " request interrupted", ex);
        }
    }

    /**
     * Sends a GET request and returns parsed response.
     */
    protected JsonNode sendGet(String endpoint) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET();

            authStrategy.apply(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_002,
                        providerName() + " GET failed (" + response.statusCode() + ")");
            }

            return objectMapper.readTree(response.body());
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_002, providerName() + " I/O error", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, providerName() + " request interrupted", ex);
        }
    }

    protected <T> T executeWithRetry(Supplier<T> call) {
        TechnicalException lastError = null;
        int maxAttempts = Math.max(1, retryAttempts);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (TechnicalException ex) {
                lastError = ex;
                if (attempt == maxAttempts) throw ex;
                sleepBackoff();
            }
        }
        throw lastError;
    }

    private void sleepBackoff() {
        if (retryBackoffMs <= 0) return;
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, providerName() + " retry interrupted", ex);
        }
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
