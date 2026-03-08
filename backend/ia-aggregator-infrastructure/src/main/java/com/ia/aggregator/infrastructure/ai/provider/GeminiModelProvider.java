package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.AiPromptRequest;
import com.ia.aggregator.application.ai.dto.AiPromptResponse;
import com.ia.aggregator.application.ai.dto.AiUsageEstimate;
import com.ia.aggregator.application.ai.port.out.AiPricingResolver;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class GeminiModelProvider extends AbstractAiProviderSupport {

    public GeminiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            AiPricingResolver pricingResolver,
            @Value("${app.ai.providers.gemini.api-key:}") String apiKey,
            @Value("${app.ai.providers.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.ai.providers.gemini.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.gemini.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.gemini.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.gemini.supported-models:gemini-1.5-flash,gemini-2.0-flash}") List<String> supportedModels
    ) {
        super(
                objectMapper,
                circuitBreakerRegistry,
                pricingResolver,
                "gemini",
                "Gemini",
                "aiProviderGemini",
                apiKey,
                baseUrl,
                timeoutMs,
                retryAttempts,
                retryBackoffMs,
                supportedModels,
                List.of("GEMINI_API_KEY"),
                false
        );
    }

    GeminiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            String apiKey,
            String baseUrl,
            long timeoutMs,
            int retryAttempts,
            long retryBackoffMs,
            List<String> supportedModels
    ) {
        this(objectMapper, circuitBreakerRegistry, AiPricingResolver.noop(), apiKey, baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
    }

    @Override
    public AiPromptResponse sendPrompt(AiPromptRequest request) {
        return executePrompt(request, () -> doSendPrompt(request));
    }

    private AiPromptResponse doSendPrompt(AiPromptRequest request) {
        long startedAt = System.currentTimeMillis();
        String model = resolveModel(request);
        String path = "/v1beta/models/" + model + ":generateContent?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpRequest httpRequest = baseJsonRequest(path, request)
                .POST(HttpRequest.BodyPublishers.ofString(writePayload(request)))
                .build();

        HttpResponse<String> response = send(httpRequest, request);
        if (response.statusCode() >= 400) {
            throw mapHttpError(request, response.statusCode(), response.body());
        }

        JsonNode root = readJson(response.body(), request, "generateContent");
        String content = readContent(root);
        if (content.isBlank()) {
            throw mapHttpError(request, 502, "Resposta sem texto legivel");
        }

        JsonNode usageNode = root.path("usageMetadata");
        AiUsageEstimate usage = new AiUsageEstimate(
                usageNode.path("promptTokenCount").asLong(0),
                usageNode.path("candidatesTokenCount").asLong(0),
                usageNode.path("totalTokenCount").asLong(0)
        );

        return buildResponse(request, content, usage, startedAt, "completed");
    }

    private String writePayload(AiPromptRequest request) {
        try {
            return objectMapper.writeValueAsString(new GeminiRequest(request.systemPrompt(), request.prompt()));
        } catch (Exception exception) {
            throw mapHttpError(request, 500, "Falha ao serializar payload do Gemini");
        }
    }

    private String readContent(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        candidates.path(0).path("content").path("parts").forEach(part -> builder.append(part.path("text").asText("")));
        return builder.toString().trim();
    }

    private record GeminiRequest(List<GeminiContent> contents, GeminiSystemInstruction systemInstruction) {
        private GeminiRequest(String systemPrompt, String prompt) {
            this(
                    List.of(new GeminiContent("user", List.of(new GeminiPart(prompt)))),
                    systemPrompt == null || systemPrompt.isBlank()
                            ? null
                            : new GeminiSystemInstruction(List.of(new GeminiPart(systemPrompt)))
            );
        }
    }

    private record GeminiSystemInstruction(List<GeminiPart> parts) {
    }

    private record GeminiContent(String role, List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }
}
