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
 * Hume AI provider — empathic text-to-speech synthesis with emotional expression.
 *
 * <p>API: {@code https://api.hume.ai}
 * <p>Supports: TEXT_TO_SPEECH
 * <p>Auth: Bearer token
 *
 * <p>TTS: POST /v0/tts
 * <p>Request: JSON {@code {"text": "...", "voice": "...", "model": "..."}}
 * <p>Response: binary audio bytes
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.hume.api-key")
public class HumeAiProvider extends AbstractAudioProvider
        implements TextToSpeechCapable {

    private static final String PROVIDER_NAME = "hume";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TEXT_TO_SPEECH);

    public HumeAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.hume.api-key:}") String apiKey,
            @Value("${app.ai.providers.hume.base-url:https://api.hume.ai}") String baseUrl,
            @Value("${app.ai.providers.hume.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.hume.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.hume.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.hume.supported-models:hume-evi2}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderHume"),
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

        String endpoint = baseUrl + "/v0/tts";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", request.text());
        body.put("model", model);
        if (request.voice() != null) {
            body.put("voice", request.voice());
        }
        if (request.outputFormat() != null) {
            body.put("output_format", request.outputFormat());
        }

        byte[] audioData = executeWithRetry(() -> sendJsonRequestForBytes(endpoint, body));

        String format = request.outputFormat() != null ? request.outputFormat() : "mp3";
        return new SynthesisResult(audioData, format, model, PROVIDER_NAME, null);
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
