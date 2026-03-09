package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.OAuth2ClientCredentialsAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractGoogleCloudProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Google Cloud Speech-to-Text provider.
 *
 * <p>API: {@code https://speech.googleapis.com/v1/speech:recognize}
 * <p>Supports: SPEECH_TO_TEXT
 * <p>Auth: OAuth2 Client Credentials
 *
 * <p>Request: {@code {"config": {"languageCode": "en-US"}, "audio": {"content": "<base64>"}}}
 * <p>Response: {@code {"results": [{"alternatives": [{"transcript": "...", "confidence": 0.98}]}]}}
 *
 * <p>Big O: O(1) per request — synchronous (for audio < 1 min; longer audio uses async).
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.google-cloud.client-id")
public class GoogleCloudSpeechProvider extends AbstractGoogleCloudProvider
        implements SpeechToTextCapable {

    private static final String PROVIDER_NAME = "google-cloud-speech";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.SPEECH_TO_TEXT);
    private static final String SPEECH_API_URL = "https://speech.googleapis.com/v1/speech:recognize";

    public GoogleCloudSpeechProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.google-cloud.token-endpoint:https://oauth2.googleapis.com/token}") String tokenEndpoint,
            @Value("${app.ai.providers.google-cloud.client-id:}") String clientId,
            @Value("${app.ai.providers.google-cloud.client-secret:}") String clientSecret,
            @Value("${app.ai.providers.google-cloud.project-id:}") String projectId,
            @Value("${app.ai.providers.google-cloud.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.google-cloud.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.google-cloud.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper,
                new OAuth2ClientCredentialsAuth(tokenEndpoint, clientId, clientSecret,
                        "https://www.googleapis.com/auth/cloud-platform"),
                projectId, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("latest_long", "latest_short", "phone_call", "command_and_search"));
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

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("languageCode", request.language() != null ? request.language() : "en-US");
        config.put("model", model);
        config.put("encoding", mapAudioEncoding(request.audioFormat()));
        if (Boolean.TRUE.equals(request.enableTimestamps())) {
            config.put("enableWordTimeOffsets", true);
        }

        Map<String, Object> audio = Map.of(
                "content", Base64.getEncoder().encodeToString(request.audioData())
        );

        Map<String, Object> body = Map.of("config", config, "audio", audio);

        JsonNode response = executeWithRetry(() -> sendJsonPost(SPEECH_API_URL, body));

        StringBuilder transcriptBuilder = new StringBuilder();
        double totalConfidence = 0;
        int altCount = 0;
        List<TranscriptionResult.WordTimestamp> words = null;

        JsonNode results = response.path("results");
        if (results.isArray()) {
            if (Boolean.TRUE.equals(request.enableTimestamps())) {
                words = new ArrayList<>();
            }
            for (JsonNode result : results) {
                JsonNode firstAlt = result.path("alternatives").path(0);
                transcriptBuilder.append(firstAlt.path("transcript").asText(""));

                double conf = firstAlt.path("confidence").asDouble(0);
                if (conf > 0) {
                    totalConfidence += conf;
                    altCount++;
                }

                if (words != null) {
                    JsonNode wordNodes = firstAlt.path("words");
                    if (wordNodes.isArray()) {
                        for (JsonNode w : wordNodes) {
                            words.add(new TranscriptionResult.WordTimestamp(
                                    w.path("word").asText(""),
                                    parseDuration(w.path("startTime").asText("0s")),
                                    parseDuration(w.path("endTime").asText("0s"))
                            ));
                        }
                    }
                }
            }
        }

        Double confidence = altCount > 0 ? totalConfidence / altCount : null;
        String language = request.language() != null ? request.language() : "en-US";

        return new TranscriptionResult(
                transcriptBuilder.toString(), language, null,
                model, PROVIDER_NAME, words, confidence
        );
    }

    private String mapAudioEncoding(String format) {
        if (format == null) return "LINEAR16";
        return switch (format.toLowerCase()) {
            case "wav", "linear16" -> "LINEAR16";
            case "flac" -> "FLAC";
            case "mp3" -> "MP3";
            case "ogg", "opus" -> "OGG_OPUS";
            case "webm" -> "WEBM_OPUS";
            default -> "LINEAR16";
        };
    }

    private double parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return 0;
        String cleaned = duration.replace("s", "");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
