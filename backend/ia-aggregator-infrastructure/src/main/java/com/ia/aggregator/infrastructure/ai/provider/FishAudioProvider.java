package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
import com.ia.aggregator.application.ai.port.out.capability.TextToSpeechCapable;
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
 * Fish Audio provider — multilingual text-to-speech and speech-to-text.
 *
 * <p>API: {@code https://api.fish.audio}
 * <p>Supports: TEXT_TO_SPEECH, SPEECH_TO_TEXT
 * <p>Auth: Bearer token
 *
 * <p>TTS: POST /v1/tts (JSON body, binary audio response)
 * <p>STT: POST /v1/asr (binary audio body, JSON response)
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.fish-audio.api-key")
public class FishAudioProvider extends AbstractAudioProvider
        implements TextToSpeechCapable, SpeechToTextCapable {

    private static final String PROVIDER_NAME = "fish-audio";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TEXT_TO_SPEECH, Capability.SPEECH_TO_TEXT);

    public FishAudioProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.fish-audio.api-key:}") String apiKey,
            @Value("${app.ai.providers.fish-audio.base-url:https://api.fish.audio}") String baseUrl,
            @Value("${app.ai.providers.fish-audio.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.fish-audio.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.fish-audio.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.fish-audio.supported-models:fish-speech-1.5,fish-speech-1.4}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderFishAudio"),
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
    public SynthesisResult synthesize(TextToSpeechRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);

        String endpoint = baseUrl + "/v1/tts";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", request.text());
        body.put("model", model);
        if (request.voice() != null) {
            body.put("voice", request.voice());
        }
        if (request.outputFormat() != null) {
            body.put("format", request.outputFormat());
        }

        byte[] audioData = executeWithRetry(() -> sendJsonRequestForBytes(endpoint, body));

        String format = request.outputFormat() != null ? request.outputFormat() : "mp3";
        return new SynthesisResult(audioData, format, model, PROVIDER_NAME, null);
    }

    @Override
    public TranscriptionResult transcribe(SpeechToTextRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);

        String endpoint = baseUrl + "/v1/asr";

        String responseJson = executeWithRetry(() ->
                sendBinaryRequest(endpoint, request.audioData(),
                        request.audioFormat() != null ? request.audioFormat() : "wav"));

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String text = parseTranscriptionResponse(root);

            String language = root.path("language").asText(null);
            double rawConf = root.path("confidence").asDouble(0);
            Double confidence = rawConf > 0 ? rawConf : null;

            return new TranscriptionResult(text, language, null, model, PROVIDER_NAME, null, confidence);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Fish Audio STT response", ex);
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
        return responseRoot.path("text").asText("");
    }
}
