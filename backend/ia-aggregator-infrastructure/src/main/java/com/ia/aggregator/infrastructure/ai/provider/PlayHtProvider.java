package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.TextToSpeechCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.auth.CompositeAuthStrategy;
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
 * PlayHT provider — ultra-realistic text-to-speech with voice cloning.
 *
 * <p>API: {@code https://api.play.ht/api/v2}
 * <p>Supports: TEXT_TO_SPEECH
 * <p>Auth: Composite — {@code X-USER-ID: {userId}} + {@code Authorization: Bearer {apiKey}}
 *
 * <p>TTS: POST /tts
 * <p>Request: JSON {@code {"text": "...", "voice": "...", "output_format": "mp3"}}
 * <p>Response: binary audio bytes
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.playht.api-key")
public class PlayHtProvider extends AbstractAudioProvider
        implements TextToSpeechCapable {

    private static final String PROVIDER_NAME = "playht";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TEXT_TO_SPEECH);

    public PlayHtProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.playht.api-key:}") String apiKey,
            @Value("${app.ai.providers.playht.user-id:}") String userId,
            @Value("${app.ai.providers.playht.base-url:https://api.play.ht/api/v2}") String baseUrl,
            @Value("${app.ai.providers.playht.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.playht.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.playht.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.playht.supported-models:playht-2.0,playht-turbo}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderPlayHt"),
                new CompositeAuthStrategy(
                        new ApiKeyHeaderAuth("X-USER-ID", userId),
                        new BearerTokenAuth(apiKey)),
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

        String endpoint = baseUrl + "/tts";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", request.text());
        body.put("model", model);
        if (request.voice() != null) {
            body.put("voice", request.voice());
        }
        String outputFormat = request.outputFormat() != null ? request.outputFormat() : "mp3";
        body.put("output_format", outputFormat);
        if (request.speed() != null) {
            body.put("speed", request.speed());
        }

        byte[] audioData = executeWithRetry(() -> sendJsonRequestForBytes(endpoint, body));

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
}
