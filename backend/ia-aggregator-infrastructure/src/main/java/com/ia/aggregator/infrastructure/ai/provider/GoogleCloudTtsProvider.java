package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.TextToSpeechCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.OAuth2ClientCredentialsAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractGoogleCloudProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Google Cloud Text-to-Speech provider.
 *
 * <p>API: {@code https://texttospeech.googleapis.com/v1/text:synthesize}
 * <p>Supports: TEXT_TO_SPEECH
 * <p>Auth: OAuth2 Client Credentials
 *
 * <p>Request: {@code {"input": {"text": "..."}, "voice": {"languageCode": "en-US"}, "audioConfig": {"audioEncoding": "MP3"}}}
 * <p>Response: {@code {"audioContent": "<base64>"}}
 *
 * <p>Big O: O(1) per request — synchronous.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.google-cloud.client-id")
public class GoogleCloudTtsProvider extends AbstractGoogleCloudProvider
        implements TextToSpeechCapable {

    private static final String PROVIDER_NAME = "google-cloud-tts";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.TEXT_TO_SPEECH);
    private static final String TTS_API_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";

    public GoogleCloudTtsProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.google-cloud.token-endpoint:https://oauth2.googleapis.com/token}") String tokenEndpoint,
            @Value("${app.ai.providers.google-cloud.client-id:}") String clientId,
            @Value("${app.ai.providers.google-cloud.client-secret:}") String clientSecret,
            @Value("${app.ai.providers.google-cloud.project-id:}") String projectId,
            @Value("${app.ai.providers.google-cloud.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.google-cloud.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.google-cloud.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper,
                new OAuth2ClientCredentialsAuth(tokenEndpoint, clientId, clientSecret,
                        "https://www.googleapis.com/auth/cloud-platform"),
                projectId, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("en-US-Neural2-C", "en-US-Neural2-D", "en-US-Studio-M", "pt-BR-Neural2-A"));
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
        String voiceName = request.voice() != null ? request.voice() : supportedModels.get(0);
        String languageCode = voiceName.length() >= 5 ? voiceName.substring(0, 5) : "en-US";

        Map<String, Object> input = Map.of("text", request.text());

        Map<String, Object> voice = new LinkedHashMap<>();
        voice.put("languageCode", languageCode);
        voice.put("name", voiceName);

        String encoding = mapEncoding(request.outputFormat());
        Map<String, Object> audioConfig = new LinkedHashMap<>();
        audioConfig.put("audioEncoding", encoding);
        if (request.speed() != null) {
            audioConfig.put("speakingRate", request.speed());
        }

        Map<String, Object> body = Map.of(
                "input", input,
                "voice", voice,
                "audioConfig", audioConfig
        );

        JsonNode response = executeWithRetry(() -> sendJsonPost(TTS_API_URL, body));

        String audioContentBase64 = response.path("audioContent").asText(null);
        if (audioContentBase64 == null) {
            throw new TechnicalException(ErrorCode.AI_014,
                    PROVIDER_NAME + " returned no audio content");
        }

        byte[] audioData = Base64.getDecoder().decode(audioContentBase64);
        String format = request.outputFormat() != null ? request.outputFormat() : "mp3";

        return new SynthesisResult(audioData, format, voiceName, PROVIDER_NAME, null);
    }

    private String mapEncoding(String format) {
        if (format == null) return "MP3";
        return switch (format.toLowerCase()) {
            case "mp3" -> "MP3";
            case "wav", "linear16" -> "LINEAR16";
            case "ogg", "opus" -> "OGG_OPUS";
            case "mulaw" -> "MULAW";
            case "alaw" -> "ALAW";
            default -> "MP3";
        };
    }
}
