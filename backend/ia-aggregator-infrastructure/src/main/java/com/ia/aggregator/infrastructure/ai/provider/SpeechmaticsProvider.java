package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractAudioProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.*;

/**
 * Speechmatics provider — enterprise speech-to-text with real-time and batch modes.
 *
 * <p>API: {@code https://asr.api.speechmatics.com}
 * <p>Supports: SPEECH_TO_TEXT
 * <p>Auth: Bearer token
 *
 * <p>STT: POST /v2/jobs (JSON body with audio reference, JSON response)
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.speechmatics.api-key")
public class SpeechmaticsProvider extends AbstractAudioProvider
        implements SpeechToTextCapable {

    private static final String PROVIDER_NAME = "speechmatics";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.SPEECH_TO_TEXT);

    public SpeechmaticsProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.speechmatics.api-key:}") String apiKey,
            @Value("${app.ai.providers.speechmatics.base-url:https://asr.api.speechmatics.com}") String baseUrl,
            @Value("${app.ai.providers.speechmatics.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.speechmatics.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.speechmatics.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.speechmatics.supported-models:speechmatics-enhanced}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderSpeechmatics"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
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

        String endpoint = baseUrl + "/v2/jobs";

        String responseJson = executeWithRetry(() ->
                sendBinaryRequest(endpoint, request.audioData(),
                        request.audioFormat() != null ? request.audioFormat() : "wav"));

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String text = parseTranscriptionResponse(root);

            String language = root.path("job").path("language").asText(null);
            double rawConf = root.path("confidence").asDouble(0);
            Double confidence = rawConf > 0 ? rawConf : null;

            return new TranscriptionResult(text, language, null, model, PROVIDER_NAME, null, confidence);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Speechmatics STT response", ex);
        }
    }

    @Override
    protected HttpRequest buildBinaryRequest(String endpoint, byte[] audioData, String audioFormat) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "audio/" + (audioFormat != null ? audioFormat : "wav"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(audioData))
                .timeout(Duration.ofMillis(timeoutMs));
        authStrategy.apply(builder);
        return builder.build();
    }

    @Override
    protected String parseTranscriptionResponse(JsonNode responseRoot) {
        JsonNode results = responseRoot.path("results");
        if (results.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode result : results) {
                String content = result.path("alternatives").path(0).path("content").asText("");
                if (!content.isEmpty()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(content);
                }
            }
            return sb.toString();
        }
        return responseRoot.path("transcript").asText("");
    }
}
