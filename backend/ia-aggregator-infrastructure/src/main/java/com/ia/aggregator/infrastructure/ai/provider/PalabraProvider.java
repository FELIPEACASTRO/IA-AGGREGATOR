package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
import com.ia.aggregator.application.ai.port.out.capability.TranslationCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractAudioProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Palabra provider — speech-to-text transcription and text translation.
 *
 * <p>API: {@code https://api.palabra.ai}
 * <p>Supports: SPEECH_TO_TEXT, TRANSLATION
 * <p>Auth: Bearer token
 *
 * <p>STT: POST /v1/transcriptions (binary audio body, JSON response)
 * <p>Translation: POST /v1/translations (JSON body, JSON response)
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.palabra.api-key")
public class PalabraProvider extends AbstractAudioProvider
        implements SpeechToTextCapable, TranslationCapable {

    private static final String PROVIDER_NAME = "palabra";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.SPEECH_TO_TEXT, Capability.TRANSLATION);

    public PalabraProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.palabra.api-key:}") String apiKey,
            @Value("${app.ai.providers.palabra.base-url:https://api.palabra.ai}") String baseUrl,
            @Value("${app.ai.providers.palabra.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.palabra.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.palabra.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.palabra.supported-models:palabra-v1}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderPalabra"),
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
    public TranscriptionResult transcribe(SpeechToTextRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);

        String endpoint = baseUrl + "/v1/transcriptions";

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
            throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Palabra STT response", ex);
        }
    }

    @Override
    public TranslationResult translate(TranslationRequest request) {
        String endpoint = baseUrl + "/v1/translations";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", request.text());
        body.put("target_lang", request.targetLang());
        if (request.sourceLang() != null) {
            body.put("source_lang", request.sourceLang());
        }

        return executeWithRetry(() -> {
            try {
                String json = objectMapper.writeValueAsString(body);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json));

                authStrategy.apply(builder);
                HttpResponse<String> response = httpClient.send(
                        builder.build(), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    throw new TechnicalException(ErrorCode.AI_016,
                            PROVIDER_NAME + " translation request failed with status " + response.statusCode());
                }

                JsonNode root = objectMapper.readTree(response.body());
                String translatedText = root.path("translated_text").asText("");
                String detectedLang = root.path("detected_source_lang").asText(null);

                return new TranslationResult(translatedText, detectedLang, PROVIDER_NAME, null);
            } catch (TechnicalException ex) {
                throw ex;
            } catch (IOException ex) {
                throw new TechnicalException(ErrorCode.AI_016,
                        "I/O error with " + PROVIDER_NAME + " translation", ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new TechnicalException(ErrorCode.AI_016,
                        PROVIDER_NAME + " translation request interrupted", ex);
            }
        });
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
