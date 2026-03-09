package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
import com.ia.aggregator.application.ai.port.out.capability.TextToSpeechCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
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
 * Deepgram provider — speech-to-text and text-to-speech.
 *
 * <p>API: {@code https://api.deepgram.com}
 * <p>Supports: SPEECH_TO_TEXT, TEXT_TO_SPEECH
 * <p>Auth: {@code Authorization: Token {key}}
 *
 * <p>STT: POST /v1/listen (binary audio body, sync response)
 * <p>TTS: POST /v1/speak (JSON body, binary audio response)
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.deepgram.api-key")
public class DeepgramProvider extends AbstractAudioProvider
        implements SpeechToTextCapable, TextToSpeechCapable {

    private static final String PROVIDER_NAME = "deepgram";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.SPEECH_TO_TEXT, Capability.TEXT_TO_SPEECH);

    public DeepgramProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.deepgram.api-key:}") String apiKey,
            @Value("${app.ai.providers.deepgram.base-url:https://api.deepgram.com}") String baseUrl,
            @Value("${app.ai.providers.deepgram.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.deepgram.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.deepgram.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.deepgram.supported-models:nova-2,nova-2-general,aura-asteria-en}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderDeepgram"),
                new ApiKeyHeaderAuth("Authorization", "Token " + apiKey),
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

        StringBuilder endpoint = new StringBuilder(baseUrl + "/v1/listen?model=" + model);
        if (request.language() != null) endpoint.append("&language=").append(request.language());
        if (Boolean.TRUE.equals(request.enableTimestamps())) endpoint.append("&utterances=true");

        String responseJson = executeWithRetry(() ->
                sendBinaryRequest(endpoint.toString(), request.audioData(),
                        request.audioFormat() != null ? request.audioFormat() : "wav"));

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String text = parseTranscriptionResponse(root);

            String language = root.path("metadata").path("language").asText(null);
            double rawDuration = root.path("metadata").path("duration").asDouble(0);
            Double duration = rawDuration > 0 ? rawDuration : null;

            JsonNode alt = root.path("results").path("channels").path(0).path("alternatives").path(0);
            double rawConf = alt.path("confidence").asDouble(0);
            Double confidence = rawConf > 0 ? rawConf : null;

            return new TranscriptionResult(text, language, duration, model, PROVIDER_NAME, null, confidence);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Deepgram STT response", ex);
        }
    }

    @Override
    public SynthesisResult synthesize(TextToSpeechRequest request) {
        String model = request.model() != null ? request.model() : "aura-asteria-en";

        String endpoint = baseUrl + "/v1/speak?model=" + model;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", request.text());

        byte[] audioData = executeWithRetry(() -> sendJsonRequestForBytes(endpoint, body));

        String format = request.outputFormat() != null ? request.outputFormat() : "mp3";
        return new SynthesisResult(audioData, format, model, PROVIDER_NAME, null);
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
                .path("results")
                .path("channels").path(0)
                .path("alternatives").path(0)
                .path("transcript").asText("");
    }
}
