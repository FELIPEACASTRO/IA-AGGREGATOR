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
 * Resemble AI provider — custom voice cloning and text-to-speech synthesis.
 *
 * <p>API: {@code https://app.resemble.ai/api/v2}
 * <p>Supports: TEXT_TO_SPEECH
 * <p>Auth: Bearer token
 *
 * <p>TTS: POST /projects/{projectId}/clips
 * <p>Request: JSON {@code {"body": "...", "voice_uuid": "...", "output_format": "mp3"}}
 * <p>Response: binary audio bytes
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.resemble.api-key")
public class ResembleAiProvider extends AbstractAudioProvider
        implements TextToSpeechCapable {

    private static final String PROVIDER_NAME = "resemble";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TEXT_TO_SPEECH);

    private final String projectId;

    public ResembleAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.resemble.api-key:}") String apiKey,
            @Value("${app.ai.providers.resemble.project-id:}") String projectId,
            @Value("${app.ai.providers.resemble.base-url:https://app.resemble.ai/api/v2}") String baseUrl,
            @Value("${app.ai.providers.resemble.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.resemble.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.resemble.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.resemble.supported-models:resemble-v2}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderResemble"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
        this.projectId = projectId;
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
        String outputFormat = request.outputFormat() != null ? request.outputFormat() : "mp3";

        String endpoint = baseUrl + "/projects/" + projectId + "/clips";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("body", request.text());
        if (request.voice() != null) {
            body.put("voice_uuid", request.voice());
        }
        body.put("output_format", outputFormat);

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
