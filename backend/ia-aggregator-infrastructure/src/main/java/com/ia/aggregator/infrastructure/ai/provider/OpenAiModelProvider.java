package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.*;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Component
public class OpenAiModelProvider implements MultiCapabilityProvider,
        ChatCapable, EmbeddingCapable, ImageGenerationCapable,
        SpeechToTextCapable, TextToSpeechCapable, ResponsesCapable {

    private static final String PROVIDER_NAME = "openai";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(
            Capability.CHAT, Capability.EMBEDDINGS, Capability.IMAGE_GENERATION,
            Capability.SPEECH_TO_TEXT, Capability.TEXT_TO_SPEECH, Capability.RESPONSES
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final String apiKey;
    private final String baseUrl;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final List<String> supportedModels;

    public OpenAiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.openai.api-key:}") String apiKey,
            @Value("${app.ai.providers.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.providers.openai.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.openai.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.openai.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.openai.supported-models:gpt-4o-mini,gpt-4.1-mini}") List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProviderOpenai");
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
        this.retryAttempts = retryAttempts;
        this.retryBackoffMs = retryBackoffMs;
        this.supportedModels = supportedModels;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public boolean supports(String model) {
        return apiKey != null
                && !apiKey.isBlank()
                && supportedModels.stream().map(String::trim).anyMatch(model::equals);
    }

    @Override
    public String generate(String prompt, String model) {
        return chat(new ChatRequest(prompt, model)).content();
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);
        return withRetry(() -> callChat(request, model));
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        String model = request.model() != null ? request.model() : "text-embedding-3-small";
        return withRetry(() -> callEmbed(request, model));
    }

    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        String model = request.model() != null ? request.model() : "dall-e-3";
        return withRetry(() -> callImageGen(request, model));
    }

    @Override
    public TranscriptionResult transcribe(SpeechToTextRequest request) {
        return withRetry(() -> callTranscribe(request));
    }

    @Override
    public SynthesisResult synthesize(TextToSpeechRequest request) {
        return withRetry(() -> callSynthesize(request));
    }

    @Override
    public ResponsesResult responses(ResponsesRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);
        return withRetry(() -> callResponses(request, model));
    }

    // ─── Chat ─────────────────────────────────────────────

    private ChatResult callChat(ChatRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            List<Map<String, String>> messages = new ArrayList<>();
            if (request.systemPrompt() != null) {
                messages.add(Map.of("role", "system", "content", request.systemPrompt()));
            }
            messages.add(Map.of("role", "user", "content", request.prompt()));
            body.put("messages", messages);
            if (request.temperature() != null) body.put("temperature", request.temperature());
            if (request.maxTokens() != null) body.put("max_tokens", request.maxTokens());

            JsonNode root = sendJsonPost(baseUrl + "/v1/chat/completions", body);

            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "OpenAI returned empty response content");
            }
            Integer promptTokens = root.path("usage").has("prompt_tokens")
                    ? root.path("usage").path("prompt_tokens").asInt() : null;
            Integer completionTokens = root.path("usage").has("completion_tokens")
                    ? root.path("usage").path("completion_tokens").asInt() : null;
            String finishReason = root.path("choices").path(0).path("finish_reason").asText(null);

            return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                    promptTokens, completionTokens, finishReason);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI chat request failed", ex);
        }
    }

    // ─── Embeddings ───────────────────────────────────────

    private EmbeddingResult callEmbed(EmbeddingRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", request.input());
            if (request.dimensions() != null) body.put("dimensions", request.dimensions());

            JsonNode root = sendJsonPost(baseUrl + "/v1/embeddings", body);

            List<float[]> embeddings = new ArrayList<>();
            int dimensions = 0;
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    JsonNode embedding = item.path("embedding");
                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = (float) embedding.get(i).asDouble();
                    }
                    embeddings.add(vector);
                    if (dimensions == 0) dimensions = vector.length;
                }
            }
            Integer totalTokens = root.path("usage").has("total_tokens")
                    ? root.path("usage").path("total_tokens").asInt() : null;

            return new EmbeddingResult(embeddings, model, PROVIDER_NAME, dimensions, totalTokens);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI embedding request failed", ex);
        }
    }

    // ─── Image Generation ─────────────────────────────────

    private ImageGenResult callImageGen(ImageGenRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("prompt", request.prompt());
            if (request.numberOfImages() != null) body.put("n", request.numberOfImages());

            String size = null;
            if (request.width() != null && request.height() != null) {
                size = request.width() + "x" + request.height();
            }
            if (size != null) body.put("size", size);
            if (request.style() != null) body.put("style", request.style());
            if (request.responseFormat() != null) body.put("response_format", request.responseFormat());

            JsonNode root = sendJsonPost(baseUrl + "/v1/images/generations", body);

            List<ImageGenResult.GeneratedImage> images = new ArrayList<>();
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode img : data) {
                    images.add(new ImageGenResult.GeneratedImage(
                            img.path("url").asText(null),
                            img.path("b64_json").asText(null),
                            img.path("revised_prompt").asText(null)
                    ));
                }
            }

            return new ImageGenResult(images, model, PROVIDER_NAME);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI image generation request failed", ex);
        }
    }

    // ─── Speech-to-Text (Whisper) ─────────────────────────

    private TranscriptionResult callTranscribe(SpeechToTextRequest request) {
        try {
            String model = request.model() != null ? request.model() : "whisper-1";
            String format = request.audioFormat() != null ? request.audioFormat() : "wav";

            String boundary = UUID.randomUUID().toString();
            byte[] multipartBody = buildMultipartBody(boundary, request.audioData(),
                    format, model, request.language());

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/audio/transcriptions"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("text").asText(null);
            if (text == null || text.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "OpenAI returned empty transcription");
            }
            String language = root.path("language").asText(request.language());
            Double duration = root.has("duration") ? root.path("duration").asDouble() : null;

            return new TranscriptionResult(text, language, duration, model, PROVIDER_NAME, null, null);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI transcription request failed", ex);
        }
    }

    private byte[] buildMultipartBody(String boundary, byte[] audioData,
                                      String format, String model, String language) throws IOException {
        var baos = new java.io.ByteArrayOutputStream();
        String crlf = "\r\n";

        baos.write(("--" + boundary + crlf).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"audio." + format + "\"" + crlf).getBytes());
        baos.write(("Content-Type: audio/" + format + crlf + crlf).getBytes());
        baos.write(audioData);
        baos.write(crlf.getBytes());

        baos.write(("--" + boundary + crlf).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"model\"" + crlf + crlf).getBytes());
        baos.write(model.getBytes());
        baos.write(crlf.getBytes());

        if (language != null) {
            baos.write(("--" + boundary + crlf).getBytes());
            baos.write(("Content-Disposition: form-data; name=\"language\"" + crlf + crlf).getBytes());
            baos.write(language.getBytes());
            baos.write(crlf.getBytes());
        }

        baos.write(("--" + boundary + crlf).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"response_format\"" + crlf + crlf).getBytes());
        baos.write("verbose_json".getBytes());
        baos.write(crlf.getBytes());

        baos.write(("--" + boundary + "--" + crlf).getBytes());
        return baos.toByteArray();
    }

    // ─── Text-to-Speech ───────────────────────────────────

    private SynthesisResult callSynthesize(TextToSpeechRequest request) {
        try {
            String model = request.model() != null ? request.model() : "tts-1";
            String voice = request.voice() != null ? request.voice() : "alloy";
            String outputFormat = request.outputFormat() != null ? request.outputFormat() : "mp3";

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", request.text());
            body.put("voice", voice);
            body.put("response_format", outputFormat);
            if (request.speed() != null) body.put("speed", request.speed());

            String payload = objectMapper.writeValueAsString(body);
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/audio/speech"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 429)
                throw new TechnicalException(ErrorCode.AI_003, "OpenAI rate limit exceeded");
            if (response.statusCode() >= 500)
                throw new TechnicalException(ErrorCode.AI_002, "OpenAI provider unavailable");
            if (response.statusCode() >= 400)
                throw new TechnicalException(ErrorCode.AI_007, "OpenAI TTS rejected: " + response.statusCode());

            return new SynthesisResult(response.body(), outputFormat, model, PROVIDER_NAME, null);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI TTS request failed", ex);
        }
    }

    // ─── Responses API ────────────────────────────────────

    private ResponsesResult callResponses(ResponsesRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", request.input());
            if (request.instructions() != null) body.put("instructions", request.instructions());
            if (request.tools() != null && !request.tools().isEmpty()) body.put("tools", request.tools());
            if (request.temperature() != null) body.put("temperature", request.temperature());
            if (request.maxOutputTokens() != null) body.put("max_output_tokens", request.maxOutputTokens());

            JsonNode root = sendJsonPost(baseUrl + "/v1/responses", body);

            List<Map<String, Object>> outputItems = new ArrayList<>();
            JsonNode output = root.path("output");
            if (output.isArray()) {
                for (JsonNode item : output) {
                    outputItems.add(objectMapper.convertValue(item, Map.class));
                }
            }

            Integer inputTokens = root.path("usage").has("input_tokens")
                    ? root.path("usage").path("input_tokens").asInt() : null;
            Integer outputTokens = root.path("usage").has("output_tokens")
                    ? root.path("usage").path("output_tokens").asInt() : null;

            return new ResponsesResult(outputItems, model, PROVIDER_NAME, inputTokens, outputTokens);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI responses request failed", ex);
        }
    }

    // ─── HTTP infrastructure ──────────────────────────────

    private <T> T withRetry(Supplier<T> call) {
        Supplier<T> guarded = CircuitBreaker.decorateSupplier(circuitBreaker, call);
        try {
            return executeWithRetry(guarded);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI circuit breaker is open", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private JsonNode sendJsonPost(String endpoint, Map<String, Object> body) {
        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);
            return objectMapper.readTree(response.body());
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_005, "Failed to parse OpenAI response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI request failed", ex);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429)
            throw new TechnicalException(ErrorCode.AI_003, "OpenAI rate limit exceeded");
        if (response.statusCode() >= 500)
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI provider unavailable");
        if (response.statusCode() >= 400)
            throw new TechnicalException(ErrorCode.AI_007, "OpenAI rejected request: " + response.statusCode());
    }

    private <T> T executeWithRetry(Supplier<T> guardedCall) {
        TechnicalException lastError = null;
        int maxAttempts = Math.max(1, retryAttempts);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return guardedCall.get();
            } catch (TechnicalException ex) {
                lastError = ex;
                if (!shouldRetry(ex) || attempt == maxAttempts) throw ex;
                sleepBackoff();
            }
        }
        throw lastError == null
                ? new TechnicalException(ErrorCode.AI_002, "OpenAI request failed after retries")
                : lastError;
    }

    private boolean shouldRetry(TechnicalException ex) {
        return ex.getErrorCode() == ErrorCode.AI_002
                || ex.getErrorCode() == ErrorCode.AI_003
                || ex.getErrorCode() == ErrorCode.AI_005;
    }

    private void sleepBackoff() {
        if (retryBackoffMs <= 0) return;
        try { Thread.sleep(retryBackoffMs); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "OpenAI retry interrupted", e);
        }
    }
}
