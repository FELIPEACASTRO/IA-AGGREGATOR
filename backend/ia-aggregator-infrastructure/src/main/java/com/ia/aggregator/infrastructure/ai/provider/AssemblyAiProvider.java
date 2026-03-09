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
 * AssemblyAI provider — speech-to-text with async polling.
 *
 * <p>API: {@code https://api.assemblyai.com}
 * <p>Supports: SPEECH_TO_TEXT
 * <p>Auth: {@code authorization: {key}} (raw key, no prefix)
 *
 * <p>Async flow:
 * <ol>
 *   <li>POST /v2/upload (binary audio) → {@code {"upload_url": "..."}}</li>
 *   <li>POST /v2/transcript (JSON) → {@code {"id": "..."}}</li>
 *   <li>GET /v2/transcript/{id} (poll) → status: queued|processing|completed|error</li>
 * </ol>
 *
 * <p>Big O: O(P) where P = poll iterations, bounded by maxPollAttempts.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.assemblyai.api-key")
public class AssemblyAiProvider extends AbstractAudioProvider
        implements SpeechToTextCapable {

    private static final String PROVIDER_NAME = "assemblyai";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.SPEECH_TO_TEXT);

    private final int maxPollAttempts;
    private final long pollIntervalMs;

    public AssemblyAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.assemblyai.api-key:}") String apiKey,
            @Value("${app.ai.providers.assemblyai.base-url:https://api.assemblyai.com}") String baseUrl,
            @Value("${app.ai.providers.assemblyai.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.assemblyai.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.assemblyai.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.assemblyai.supported-models:best,nano}") List<String> supportedModels,
            @Value("${app.ai.providers.assemblyai.max-poll-attempts:60}") int maxPollAttempts,
            @Value("${app.ai.providers.assemblyai.poll-interval-ms:3000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderAssemblyAi"),
                new ApiKeyHeaderAuth("authorization", apiKey),
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

            String uploadUrl;
            try {
                uploadUrl = objectMapper.readTree(uploadJson).path("upload_url").asText();
            } catch (Exception ex) {
                throw new TechnicalException(ErrorCode.AI_016, "Failed to parse AssemblyAI upload response", ex);
            }

            // Step 2: Create transcript request
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("audio_url", uploadUrl);
            body.put("speech_model", model);
            if (request.language() != null) body.put("language_code", request.language());
            if (Boolean.TRUE.equals(request.enableTimestamps())) body.put("word_boost", List.of());

            String transcriptJson = sendJsonPost(baseUrl + "/v2/transcript", body);
            String transcriptId;
            try {
                transcriptId = objectMapper.readTree(transcriptJson).path("id").asText();
            } catch (Exception ex) {
                throw new TechnicalException(ErrorCode.AI_016, "Failed to parse AssemblyAI transcript response", ex);
            }

            // Step 3: Poll until complete
            JsonNode result = pollUntilComplete(baseUrl + "/v2/transcript/" + transcriptId);

            String text = result.path("text").asText("");
            String language = result.path("language_code").asText(null);
            double rawDuration = result.path("audio_duration").asDouble(0);
            Double duration = rawDuration > 0 ? rawDuration : null;
            double rawConf = result.path("confidence").asDouble(0);
            Double confidence = rawConf > 0 ? rawConf : null;

            return new TranscriptionResult(text, language, duration, model, PROVIDER_NAME, null, confidence);
        });
    }

    @Override
    protected HttpRequest buildBinaryRequest(String endpoint, byte[] audioData, String audioFormat) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(audioData));

        authStrategy.apply(builder);
        return builder.build();
    }

    @Override
    protected String parseTranscriptionResponse(JsonNode responseRoot) {
        return responseRoot.path("text").asText("");
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

                if ("completed".equals(status)) {
                    return root;
                }
                if ("error".equals(status)) {
                    String error = root.path("error").asText("Unknown error");
                    throw new TechnicalException(ErrorCode.AI_016,
                            PROVIDER_NAME + " transcription failed: " + error);
                }

                // queued or processing — wait and retry
                Thread.sleep(pollIntervalMs);
            } catch (TechnicalException ex) {
                throw ex;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new TechnicalException(ErrorCode.AI_016, PROVIDER_NAME + " polling interrupted", ex);
            } catch (Exception ex) {
                throw new TechnicalException(ErrorCode.AI_016, "Failed to parse poll response", ex);
            }
        }

        throw new TechnicalException(ErrorCode.AI_004,
                PROVIDER_NAME + " transcription timed out after " + maxPollAttempts + " polls");
    }
}
