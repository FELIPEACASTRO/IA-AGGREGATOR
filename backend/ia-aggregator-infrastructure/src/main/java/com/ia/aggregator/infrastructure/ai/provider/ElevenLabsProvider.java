package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
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
 * ElevenLabs provider — high-quality text-to-speech synthesis.
 *
 * <p>API: {@code https://api.elevenlabs.io}
 * <p>Supports: TEXT_TO_SPEECH
 * <p>Auth: {@code xi-api-key: {key}}
 *
 * <p>TTS: POST /v1/text-to-speech/{voiceId}
 * <p>Request: JSON {@code {"text": "...", "model_id": "..."}}
 * <p>Response: binary audio (audio/mpeg)
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.elevenlabs.api-key")
public class ElevenLabsProvider extends AbstractAudioProvider
        implements TextToSpeechCapable {

    private static final String PROVIDER_NAME = "elevenlabs";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TEXT_TO_SPEECH);
    private static final String DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM";

    public ElevenLabsProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.elevenlabs.api-key:}") String apiKey,
            @Value("${app.ai.providers.elevenlabs.base-url:https://api.elevenlabs.io}") String baseUrl,
            @Value("${app.ai.providers.elevenlabs.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.elevenlabs.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.elevenlabs.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.elevenlabs.supported-models:eleven_multilingual_v2,eleven_monolingual_v1,eleven_turbo_v2_5}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderElevenLabs"),
                new ApiKeyHeaderAuth("xi-api-key", apiKey),
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
        String voiceId = request.voice() != null ? request.voice() : DEFAULT_VOICE_ID;

        String endpoint = baseUrl + "/v1/text-to-speech/" + voiceId;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", request.text());
        body.put("model_id", model);

        if (request.speed() != null) {
            Map<String, Object> voiceSettings = new LinkedHashMap<>();
            voiceSettings.put("stability", 0.5);
            voiceSettings.put("similarity_boost", 0.75);
            body.put("voice_settings", voiceSettings);
        }

        String outputFormat = request.outputFormat() != null ? request.outputFormat() : "mp3";
        String endpointWithFormat = endpoint + "?output_format=" + mapOutputFormat(outputFormat);

        byte[] audioData = executeWithRetry(() -> sendJsonRequestForBytes(endpointWithFormat, body));

        return new SynthesisResult(audioData, outputFormat, model, PROVIDER_NAME, null);
    }

    @Override
    protected HttpRequest buildBinaryRequest(String endpoint, byte[] audioData, String audioFormat) {
        throw new TechnicalException(ErrorCode.AI_007,
                PROVIDER_NAME + " does not support binary audio upload (TTS-only provider)");
    }

    @Override
    protected String parseTranscriptionResponse(JsonNode responseRoot) {
        throw new TechnicalException(ErrorCode.AI_007,
                PROVIDER_NAME + " does not support STT (TTS-only provider)");
    }

    private String mapOutputFormat(String format) {
        return switch (format.toLowerCase()) {
            case "mp3" -> "mp3_44100_128";
            case "pcm" -> "pcm_16000";
            case "opus" -> "opus_16000";
            case "ulaw" -> "ulaw_8000";
            default -> "mp3_44100_128";
        };
    }
}
