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

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class OpenAiModelProvider extends AbstractAiProviderSupport {

    public OpenAiModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            AiPricingResolver pricingResolver,
            @Value("${app.ai.providers.openai.api-key:}") String apiKey,
            @Value("${app.ai.providers.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.providers.openai.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.openai.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.openai.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.openai.supported-models:gpt-4o-mini,gpt-4.1-mini}") List<String> supportedModels
    ) {
        super(
                objectMapper,
                circuitBreakerRegistry,
                pricingResolver,
                "openai",
                "OpenAI",
                "aiProviderOpenai",
                apiKey,
                baseUrl,
                timeoutMs,
                retryAttempts,
                retryBackoffMs,
                supportedModels,
                List.of("OPENAI_API_KEY"),
                false
        );
    }

    OpenAiModelProvider(
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

        HttpRequest httpRequest = baseJsonRequest("/v1/responses", request)
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(writePayload(request, model)))
                .build();

        HttpResponse<String> response = send(httpRequest, request);
        if (response.statusCode() >= 400) {
            throw mapHttpError(request, response.statusCode(), response.body());
        }

        JsonNode root = readJson(response.body(), request, "responses");
        String content = readOutputText(root);
        if (content.isBlank()) {
            throw mapHttpError(request, 502, "Resposta sem texto legivel");
        }

        JsonNode usageNode = root.path("usage");
        AiUsageEstimate usage = new AiUsageEstimate(
                usageNode.path("input_tokens").asLong(0),
                usageNode.path("output_tokens").asLong(0),
                usageNode.path("total_tokens").asLong(0)
        );

        return buildResponse(request, content, usage, startedAt, root.path("status").asText("completed"));
    }

    private String writePayload(AiPromptRequest request, String model) {
        try {
            return objectMapper.writeValueAsString(new OpenAiResponsesRequest(
                    model,
                    request.systemPrompt(),
                    request.prompt(),
                    request.temperature(),
                    request.maxTokens()
            ));
        } catch (Exception exception) {
            throw mapHttpError(request, 500, "Falha ao serializar payload da OpenAI");
        }
    }

    private String readOutputText(JsonNode root) {
        String direct = root.path("output_text").asText("");
        if (!direct.isBlank()) {
            return direct.trim();
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            StringBuilder builder = new StringBuilder();
            output.forEach(item -> item.path("content").forEach(content -> {
                if ("output_text".equals(content.path("type").asText(""))) {
                    builder.append(content.path("text").asText(""));
                }
            }));
            return builder.toString().trim();
        }

        return "";
    }

    private record OpenAiResponsesRequest(
            String model,
            List<OpenAiInputMessage> input,
            Double temperature,
            Integer max_output_tokens
    ) {
        private OpenAiResponsesRequest(String model,
                                       String systemPrompt,
                                       String prompt,
                                       Double temperature,
                                       Integer maxOutputTokens) {
            this(
                    model,
                    systemPrompt == null || systemPrompt.isBlank()
                            ? List.of(new OpenAiInputMessage("user", prompt))
                            : List.of(
                                    new OpenAiInputMessage("system", systemPrompt),
                                    new OpenAiInputMessage("user", prompt)
                            ),
                    temperature,
                    maxOutputTokens
            );
        }
    }

    private record OpenAiInputMessage(String role, String content) {
    }
}
