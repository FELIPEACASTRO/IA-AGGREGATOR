package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.application.ai.port.out.capability.ChatCapable;
import com.ia.aggregator.application.ai.port.out.capability.SpeechToTextCapable;
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
public class GroqModelProvider implements MultiCapabilityProvider,
        ChatCapable, SpeechToTextCapable {

    private static final String PROVIDER_NAME = "groq";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.SPEECH_TO_TEXT);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final CircuitBreaker circuitBreaker;
    private final String apiKey;
    private final String baseUrl;
    private final long timeoutMs;
    private final int retryAttempts;
    private final long retryBackoffMs;
    private final List<String> supportedModels;

    public GroqModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.groq.api-key:}") String apiKey,
            @Value("${app.ai.providers.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${app.ai.providers.groq.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.groq.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.groq.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.groq.supported-models:llama-3.1-8b-instant,llama-3.1-70b-versatile}") List<String> supportedModels
    ) {
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiProviderGroq");
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
        Supplier<ChatResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callChat(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Groq circuit breaker is open", ex);
        }
    }

    @Override
    public TranscriptionResult transcribe(SpeechToTextRequest request) {
        Supplier<TranscriptionResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callTranscribe(request));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Groq circuit breaker is open", ex);
        }
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

            JsonNode root = sendJsonPost(baseUrl + "/chat/completions", body);

            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new TechnicalException(ErrorCode.AI_005, "Groq returned empty response content");
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
            throw new TechnicalException(ErrorCode.AI_002, "Groq chat request failed", ex);
        }
    }

    // ─── Speech-to-Text (Whisper via Groq) ────────────────

    private TranscriptionResult callTranscribe(SpeechToTextRequest request) {
        try {
            String model = request.model() != null ? request.model() : "whisper-large-v3";
            String format = request.audioFormat() != null ? request.audioFormat() : "wav";

            // Groq Whisper API uses multipart/form-data (OpenAI-compatible)
            String boundary = UUID.randomUUID().toString();
            byte[] multipartBody = buildMultipartBody(boundary, request.audioData(),
                    format, model, request.language());

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/audio/transcriptions"))
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
                throw new TechnicalException(ErrorCode.AI_005, "Groq returned empty transcription");
            }
            String language = root.path("language").asText(request.language());
            Double duration = root.has("duration") ? root.path("duration").asDouble() : null;

            return new TranscriptionResult(text, language, duration, model, PROVIDER_NAME, null, null);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Groq transcription request failed", ex);
        }
    }

    private byte[] buildMultipartBody(String boundary, byte[] audioData,
                                      String format, String model, String language) throws IOException {
        var baos = new java.io.ByteArrayOutputStream();
        String crlf = "\r\n";

        // file part
        baos.write(("--" + boundary + crlf).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"audio." + format + "\"" + crlf).getBytes());
        baos.write(("Content-Type: audio/" + format + crlf + crlf).getBytes());
        baos.write(audioData);
        baos.write(crlf.getBytes());

        // model part
        baos.write(("--" + boundary + crlf).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"model\"" + crlf + crlf).getBytes());
        baos.write(model.getBytes());
        baos.write(crlf.getBytes());

        // language part (optional)
        if (language != null) {
            baos.write(("--" + boundary + crlf).getBytes());
            baos.write(("Content-Disposition: form-data; name=\"language\"" + crlf + crlf).getBytes());
            baos.write(language.getBytes());
            baos.write(crlf.getBytes());
        }

        // response_format
        baos.write(("--" + boundary + crlf).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"response_format\"" + crlf + crlf).getBytes());
        baos.write("verbose_json".getBytes());
        baos.write(crlf.getBytes());

        baos.write(("--" + boundary + "--" + crlf).getBytes());
        return baos.toByteArray();
    }

    // ─── HTTP infrastructure ──────────────────────────────

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
            throw new TechnicalException(ErrorCode.AI_005, "Failed to parse Groq response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002, "Groq request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_002, "Groq request failed", ex);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429)
            throw new TechnicalException(ErrorCode.AI_003, "Groq rate limit exceeded");
        if (response.statusCode() >= 500)
            throw new TechnicalException(ErrorCode.AI_002, "Groq provider unavailable");
        if (response.statusCode() >= 400)
            throw new TechnicalException(ErrorCode.AI_007, "Groq rejected request: " + response.statusCode());
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
                ? new TechnicalException(ErrorCode.AI_002, "Groq request failed after retries")
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
            throw new TechnicalException(ErrorCode.AI_002, "Groq retry interrupted", e);
        }
    }
}
