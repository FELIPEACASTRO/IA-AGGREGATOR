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
 * Abstract base for Google Cloud AI providers (Vision, Speech, NLP, TTS, Translation).
 *
 * <p>Design Pattern: Template Method — subclasses implement service-specific logic
 * while this base handles OAuth2 auth, JSON request/response, error parsing, and retry.
 *
 * <p>All Google Cloud APIs follow a similar pattern:
 * <ul>
 *   <li>REST JSON over HTTPS</li>
 *   <li>OAuth2 Bearer token authentication</li>
 *   <li>Error format: {@code {"error": {"code": N, "message": "...", "status": "..."}}}
 * </ul>
 *
 * <p>Big O: O(1) per request — all Google Cloud APIs used here are synchronous.
 */
public abstract class AbstractGoogleCloudProvider implements MultiCapabilityProvider {

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final AuthStrategy authStrategy;
    protected final String projectId;
    protected final long timeoutMs;
    protected final int retryAttempts;
    protected final long retryBackoffMs;
    protected final List<String> supportedModels;

    protected AbstractGoogleCloudProvider(
            ObjectMapper objectMapper,
            AuthStrategy authStrategy,
            String projectId,
            long timeoutMs,
            int retryAttempts,
            long retryBackoffMs,
            List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.authStrategy = authStrategy;
        this.projectId = projectId;
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
                providerName() + " does not support generic text generation. Use specific capability methods.");
    }

    /**
     * Sends a JSON POST request and returns the parsed response.
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
                throw new TechnicalException(ErrorCode.AI_003,
                        providerName() + " rate limit exceeded");
            }
            if (response.statusCode() >= 400) {
                String errorMsg = parseGoogleError(response.body());
                throw new TechnicalException(ErrorCode.AI_002,
                        providerName() + " request failed (" + response.statusCode() + "): " + errorMsg);
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
     * Sends a JSON POST request and returns raw bytes.
     * Used for TTS (synthesize returns audio bytes inline as base64 or binary).
     */
    protected String sendJsonPostRaw(String endpoint, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            authStrategy.apply(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                String errorMsg = parseGoogleError(response.body());
                throw new TechnicalException(ErrorCode.AI_002,
                        providerName() + " request failed (" + response.statusCode() + "): " + errorMsg);
            }

            return response.body();
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

    private String parseGoogleError(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                return error.path("message").asText("Unknown Google Cloud error");
            }
            return body.length() > 200 ? body.substring(0, 200) : body;
        } catch (Exception ex) {
            return body != null && body.length() > 200 ? body.substring(0, 200) : String.valueOf(body);
        }
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
}
