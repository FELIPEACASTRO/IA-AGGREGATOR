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
 * Rev AI provider — speech-to-text transcription with high accuracy.
 *
 * <p>API: {@code https://api.rev.ai}
 * <p>Supports: SPEECH_TO_TEXT
 * <p>Auth: Bearer token
 *
 * <p>STT: POST /speechtotext/v1/jobs (binary audio body, JSON response)
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.rev-ai.api-key")
public class RevAiProvider extends AbstractAudioProvider
        implements SpeechToTextCapable {

    private static final String PROVIDER_NAME = "rev-ai";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.SPEECH_TO_TEXT);

    public RevAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.rev-ai.api-key:}") String apiKey,
            @Value("${app.ai.providers.rev-ai.base-url:https://api.rev.ai}") String baseUrl,
            @Value("${app.ai.providers.rev-ai.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.rev-ai.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.rev-ai.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.rev-ai.supported-models:rev-ai-english}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderRevAi"),
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

        String endpoint = baseUrl + "/speechtotext/v1/jobs";

        String responseJson = executeWithRetry(() ->
                sendBinaryRequest(endpoint, request.audioData(),
                        request.audioFormat() != null ? request.audioFormat() : "wav"));

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String text = parseTranscriptionResponse(root);

            String language = root.path("language").asText(null);
            double rawConf = root.path("confidence").asDouble(0);
            Double confidence = rawConf > 0 ? rawConf : null;
            double rawDuration = root.path("duration_seconds").asDouble(0);
            Double duration = rawDuration > 0 ? rawDuration : null;

            return new TranscriptionResult(text, language, duration, model, PROVIDER_NAME, null, confidence);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Rev AI STT response", ex);
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
        JsonNode monologues = responseRoot.path("monologues");
        if (monologues.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode monologue : monologues) {
                JsonNode elements = monologue.path("elements");
                if (elements.isArray()) {
                    for (JsonNode element : elements) {
                        String value = element.path("value").asText("");
                        sb.append(value);
                    }
                }
            }
            return sb.toString().trim();
        }
        return responseRoot.path("text").asText("");
    }
}
