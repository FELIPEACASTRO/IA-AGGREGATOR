package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
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
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Azure Cognitive Services Speech provider — STT and TTS via Azure Speech API.
 *
 * <p>API: {@code https://{region}.api.cognitive.microsoft.com}
 * <p>Supports: SPEECH_TO_TEXT, TEXT_TO_SPEECH
 * <p>Auth: {@code Ocp-Apim-Subscription-Key: {key}}
 *
 * <p>TTS: POST /cognitiveservices/v1 (SSML body, binary audio response)
 * <p>STT: POST /speechtotext/v3.1/transcriptions (binary audio body, JSON response)
 *
 * <p>Big O: O(1) per request — synchronous, no polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.azure-speech.api-key")
public class AzureSpeechProvider extends AbstractAudioProvider
        implements SpeechToTextCapable, TextToSpeechCapable {

    private static final String PROVIDER_NAME = "azure-speech";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.SPEECH_TO_TEXT, Capability.TEXT_TO_SPEECH);
    private static final String DEFAULT_VOICE = "en-US-JennyNeural";

    private final String region;

    public AzureSpeechProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.azure-speech.api-key:}") String apiKey,
            @Value("${app.ai.providers.azure-speech.region:eastus}") String region,
            @Value("${app.ai.providers.azure-speech.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.azure-speech.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.azure-speech.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.azure-speech.supported-models:azure-tts,azure-stt}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderAzureSpeech"),
                new ApiKeyHeaderAuth("Ocp-Apim-Subscription-Key", apiKey),
                "https://" + region + ".api.cognitive.microsoft.com",
                timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
        this.region = region;
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
        String voice = request.voice() != null ? request.voice() : DEFAULT_VOICE;
        String outputFormat = request.outputFormat() != null ? request.outputFormat() : "mp3";

        String endpoint = baseUrl + "/cognitiveservices/v1";

        String ssml = buildSsml(request.text(), voice);

        byte[] audioData = executeWithRetry(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofMillis(timeoutMs))
                        .header("Content-Type", "application/ssml+xml")
                        .header("X-Microsoft-OutputFormat", mapOutputFormat(outputFormat))
                        .POST(HttpRequest.BodyPublishers.ofString(ssml));

                authStrategy.apply(builder);

                HttpResponse<byte[]> response = httpClient.send(
                        builder.build(), HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() >= 400) {
                    throw new TechnicalException(ErrorCode.AI_014,
                            PROVIDER_NAME + " TTS request failed with status " + response.statusCode());
                }

                return response.body();
            } catch (TechnicalException ex) {
                throw ex;
            } catch (java.io.IOException ex) {
                throw new TechnicalException(ErrorCode.AI_014,
                        "I/O error with " + PROVIDER_NAME, ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new TechnicalException(ErrorCode.AI_014,
                        PROVIDER_NAME + " TTS request interrupted", ex);
            }
        });

        return new SynthesisResult(audioData, outputFormat, model, PROVIDER_NAME, null);
    }

    @Override
    public TranscriptionResult transcribe(SpeechToTextRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);

        String endpoint = baseUrl + "/speechtotext/v3.1/transcriptions";

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
            throw new TechnicalException(ErrorCode.AI_016, "Failed to parse Azure Speech STT response", ex);
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
        return responseRoot.path("combinedRecognizedPhrases").path(0)
                .path("display").asText("");
    }

    private String buildSsml(String text, String voice) {
        return "<speak version='1.0' xml:lang='en-US'>"
                + "<voice name='" + voice + "'>"
                + escapeXml(text)
                + "</voice></speak>";
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String mapOutputFormat(String format) {
        return switch (format.toLowerCase()) {
            case "mp3" -> "audio-16khz-128kbitrate-mono-mp3";
            case "wav" -> "riff-16khz-16bit-mono-pcm";
            case "opus" -> "ogg-16khz-16bit-mono-opus";
            case "ogg" -> "ogg-16khz-16bit-mono-opus";
            default -> "audio-16khz-128kbitrate-mono-mp3";
        };
    }
}
