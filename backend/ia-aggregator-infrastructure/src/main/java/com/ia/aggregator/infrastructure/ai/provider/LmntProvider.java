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
 * LMNT provider — real-time text-to-speech synthesis with voice cloning.
 *
 * <p>API: {@code https://api.lmnt.com}
 * <p>Supports: TEXT_TO_SPEECH
 * <p>Auth: Bearer token
 *
 * <p>TTS: POST /v1/ai/speech
 * <p>Request: JSON {@code {"text": "...", "voice": "...", "model": "..."}}
 * <p>Response: binary audio bytes
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.lmnt.api-key")
public class LmntProvider extends AbstractAudioProvider
        implements TextToSpeechCapable {

    private static final String PROVIDER_NAME = "lmnt";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TEXT_TO_SPEECH);

    public LmntProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.lmnt.api-key:}") String apiKey,
            @Value("${app.ai.providers.lmnt.base-url:https://api.lmnt.com}") String baseUrl,
            @Value("${app.ai.providers.lmnt.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.lmnt.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.lmnt.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.lmnt.supported-models:lmnt-1}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderLmnt"),
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

        String endpoint = baseUrl + "/v1/ai/speech";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", request.text());
        body.put("model", model);
        if (request.voice() != null) {
            body.put("voice", request.voice());
        }
        if (request.outputFormat() != null) {
            body.put("format", request.outputFormat());
        }
        if (request.speed() != null) {
            body.put("speed", request.speed());
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
