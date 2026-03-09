package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractAudioProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Gladia provider — GDPR/HIPAA compliant speech-to-text with async polling.
 *
 * <p>API: {@code https://api.gladia.io}
 * <p>Supports: SPEECH_TO_TEXT
 * <p>Auth: {@code x-gladia-key: {key}}
 *
 * <p>Async flow:
 * <ol>
 *   <li>POST /v2/upload (binary audio) → {@code {"audio_url": "..."}}</li>
 *   <li>POST /v2/transcription (JSON) → {@code {"id": "...", "result_url": "..."}}</li>
 *   <li>GET {result_url} (poll) → status: queued|processing|done|error</li>
 * </ol>
 *
 * <p>Big O: O(P) where P = poll iterations, bounded by maxPollAttempts.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.gladia.api-key")
public class GladiaProvider extends AbstractAudioProvider
        implements SpeechToTextCapable {

    private static final String PROVIDER_NAME = "gladia";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.SPEECH_TO_TEXT);

    private final int maxPollAttempts;
    private final long pollIntervalMs;

    public GladiaProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.gladia.api-key:}") String apiKey,
            @Value("${app.ai.providers.gladia.base-url:https://api.gladia.io}") String baseUrl,
            @Value("${app.ai.providers.gladia.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.gladia.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.gladia.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.gladia.supported-models:enhanced,fast}") List<String> supportedModels,
            @Value("${app.ai.providers.gladia.max-poll-attempts:60}") int maxPollAttempts,
            @Value("${app.ai.providers.gladia.poll-interval-ms:3000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderGladia"),
                new ApiKeyHeaderAuth("x-gladia-key", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
        this.maxPollAttempts = maxPollAttempts;
        this.pollIntervalMs = pollIntervalMs;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<Capability> capabilities() {
        return SUPPORTED_CAPABILITIES;
    }

    @Override
    public TranscriptionResult transcribe(SpeechToTextRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);

        return executeWithRetry(() -> {
            // Step 1: Upload audio binary
            String uploadJson = sendBinaryRequest(
                    baseUrl + "/v2/upload",
                    request.audioData(),
                    request.audioFormat() != null ? request.audioFormat() : "wav");

            String audioUrl;
            try {
                audioUrl = objectMapper.readTree(uploadJson).path("audio_url").asText();
            } catch (Exception ex) {
                throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Gladia upload response", ex);
            }

            // Step 2: Create transcription request
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("audio_url", audioUrl);
            if (request.language() != null) body.put("language", request.language());
            if (Boolean.TRUE.equals(request.enableTimestamps())) body.put("subtitles", true);

            String transcriptJson = sendJsonPost(baseUrl + "/v2/transcription", body);
            String resultUrl;
            try {
                resultUrl = objectMapper.readTree(transcriptJson).path("result_url").asText();
            } catch (Exception ex) {
                throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Gladia transcription response", ex);
            }

            // Step 3: Poll until complete
            JsonNode result = pollUntilComplete(resultUrl);

            String text = parseTranscriptionResponse(result);
            String language = result.path("result").path("transcription")
                    .path("languages").path(0).asText(null);

            return new TranscriptionResult(text, language, null, model, PROVIDER_NAME, null, null);
        });
    }

    @Override
    protected HttpRequest buildBinaryRequest(String endpoint, byte[] audioData, String audioFormat) {
        String contentType = "audio/" + (audioFormat != null ? audioFormat : "wav");

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(audioData));

        authStrategy.apply(builder);
        return builder.build();
    }

    @Override
    protected String parseTranscriptionResponse(JsonNode responseRoot) {
        return responseRoot
                .path("result")
                .path("transcription")
                .path("full_transcript").asText("");
    }

    // ---------- Async helpers ----------

    private String sendJsonPost(String endpoint, Object payload) {
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
                throw new TechnicalException(ErrorCode.AI_016,
                        PROVIDER_NAME + " JSON POST failed: " + response.statusCode());
            }
            return response.body();
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_016, PROVIDER_NAME + " I/O error", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_016, PROVIDER_NAME + " request interrupted", ex);
        }
    }

    private String sendGet(String endpoint) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET();

            authStrategy.apply(builder);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_016,
                        PROVIDER_NAME + " GET failed: " + response.statusCode());
            }
            return response.body();
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_016, PROVIDER_NAME + " I/O error", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_016, PROVIDER_NAME + " request interrupted", ex);
        }
    }

    private JsonNode pollUntilComplete(String endpoint) {
        for (int attempt = 1; attempt <= maxPollAttempts; attempt++) {
            String json = sendGet(endpoint);
            try {
                JsonNode root = objectMapper.readTree(json);
                String status = root.path("status").asText("queued");

                if ("done".equals(status)) {
                    return root;
                }
                if ("error".equals(status)) {
                    String error = root.path("error_message").asText("Unknown error");
                    throw new TechnicalException(ErrorCode.AI_016,
                            PROVIDER_NAME + " transcription failed: " + error);
                }

                Thread.sleep(pollIntervalMs);
            } catch (TechnicalException ex) {
                throw ex;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new TechnicalException(ErrorCode.AI_016, PROVIDER_NAME + " polling interrupted", ex);
            } catch (Exception ex) {
                throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Gladia poll response", ex);
            }
        }

        throw new TechnicalException(ErrorCode.AI_004,
                PROVIDER_NAME + " transcription timed out after " + maxPollAttempts + " polls");
    }
}
