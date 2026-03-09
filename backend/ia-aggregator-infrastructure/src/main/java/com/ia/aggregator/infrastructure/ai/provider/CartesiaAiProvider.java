package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
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
 * Cartesia AI provider — ultra-low-latency text-to-speech synthesis.
 *
 * <p>API: {@code https://api.cartesia.ai}
 * <p>Supports: TEXT_TO_SPEECH
 * <p>Auth: Bearer token
 *
 * <p>TTS: POST /tts/bytes
 * <p>Request: JSON {@code {"model_id": "...", "transcript": "...", "voice": {"mode": "id", "id": "..."}, "output_format": {"container": "mp3"}}}
 * <p>Response: binary audio bytes
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.cartesia.api-key")
public class CartesiaAiProvider extends AbstractAudioProvider
        implements TextToSpeechCapable {

    private static final String PROVIDER_NAME = "cartesia";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TEXT_TO_SPEECH);
    private static final String DEFAULT_VOICE_ID = "a0e99841-438c-4a64-b679-ae501e7d6091";

    public CartesiaAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.cartesia.api-key:}") String apiKey,
            @Value("${app.ai.providers.cartesia.base-url:https://api.cartesia.ai}") String baseUrl,
            @Value("${app.ai.providers.cartesia.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.cartesia.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.cartesia.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.cartesia.supported-models:sonic-2,sonic-1}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderCartesia"),
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
        String voiceId = request.voice() != null ? request.voice() : DEFAULT_VOICE_ID;

        String endpoint = baseUrl + "/tts/bytes";

        Map<String, Object> voice = new LinkedHashMap<>();
        voice.put("mode", "id");
        voice.put("id", voiceId);

        String container = request.outputFormat() != null ? request.outputFormat() : "mp3";
        Map<String, Object> outputFormat = new LinkedHashMap<>();
        outputFormat.put("container", container);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model_id", model);
        body.put("transcript", request.text());
        body.put("voice", voice);
        body.put("output_format", outputFormat);

        byte[] audioData = executeWithRetry(() -> sendJsonRequestForBytes(endpoint, body));

        return new SynthesisResult(audioData, container, model, PROVIDER_NAME, null);
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
}
