package com.ia.aggregator.infrastructure.ai.provider.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.infrastructure.ai.auth.AuthStrategy;
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
 * Abstract base for audio providers (STT/TTS).
 * Handles binary I/O (audio bytes upload/download), content-type management.
 *
 * <p>Design Pattern: Template Method — subclasses override:
 * <ul>
 *   <li>{@link #buildBinaryRequest(String, byte[], String)} — custom multipart/binary request</li>
 *   <li>{@link #parseTranscriptionResponse(JsonNode)} — custom response parsing for STT</li>
 * </ul>
 */
public abstract class AbstractAudioProvider implements MultiCapabilityProvider {

    protected final ObjectMapper objectMapper;
    protected final HttpClient httpClient;
    protected final CircuitBreaker circuitBreaker;
    protected final AuthStrategy authStrategy;
    protected final String baseUrl;
    protected final long timeoutMs;
    protected final int retryAttempts;
    protected final long retryBackoffMs;
    protected final List<String> supportedModels;

    protected AbstractAudioProvider(
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
        throw new TechnicalException(ErrorCode.AI_007,
                providerName() + " does not support text generation via generate(). Use specific audio methods.");
    }

    // ---------- Abstract template methods ----------

    /**
     * Builds a binary upload request.
     *
     * @param endpoint the target URL
     * @param audioData the raw audio bytes
     * @param audioFormat the audio format (e.g., "wav", "mp3")
     * @return the configured HttpRequest
     */
    protected abstract HttpRequest buildBinaryRequest(String endpoint, byte[] audioData, String audioFormat);

    /**
     * Parses the transcription response from the provider.
     */
    protected abstract String parseTranscriptionResponse(JsonNode responseRoot);

    // ---------- Core execution methods ----------

    /**
     * Sends binary audio data and returns the JSON response body.
     */
    protected String sendBinaryRequest(String endpoint, byte[] audioData, String audioFormat) {
        try {
            HttpRequest request = buildBinaryRequest(endpoint, audioData, audioFormat);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                throw new TechnicalException(ErrorCode.AI_003,
                        providerName() + " rate limit exceeded");
            }
            if (response.statusCode() >= 500) {
                throw new TechnicalException(ErrorCode.AI_002,
                        providerName() + " server error: " + response.statusCode());
            }
            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_016,
                        providerName() + " binary request failed with status " + response.statusCode());
            }

            return response.body();
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_016,
                    "Binary I/O error with " + providerName(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_016,
                    providerName() + " binary request interrupted", ex);
        }
    }

    /**
     * Sends a JSON POST request and returns raw audio bytes.
     * Used for TTS (text → audio).
     */
    protected byte[] sendJsonRequestForBytes(String endpoint, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            authStrategy.apply(requestBuilder);

            HttpResponse<byte[]> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_014,
                        providerName() + " TTS request failed with status " + response.statusCode());
            }

            return response.body();
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_014,
                    "Binary I/O error with " + providerName(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_014,
                    providerName() + " TTS request interrupted", ex);
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
            throw new TechnicalException(ErrorCode.AI_016,
                    providerName() + " retry interrupted", ex);
        }
    }
}
