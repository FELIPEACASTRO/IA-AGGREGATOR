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
public class AnthropicModelProvider extends AbstractAiProviderSupport {

    private final int defaultMaxTokens;
    private final String anthropicVersion;

    public AnthropicModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            AiPricingResolver pricingResolver,
            @Value("${app.ai.providers.anthropic.api-key:}") String apiKey,
            @Value("${app.ai.providers.anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${app.ai.providers.anthropic.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.anthropic.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.anthropic.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.anthropic.max-tokens:1024}") int defaultMaxTokens,
            @Value("${app.ai.providers.anthropic.supported-models:claude-3-5-haiku-latest,claude-3-7-sonnet-latest}") List<String> supportedModels,
            @Value("${app.ai.providers.anthropic.version:2023-06-01}") String anthropicVersion
    ) {
        super(
                objectMapper,
                circuitBreakerRegistry,
                pricingResolver,
                "anthropic",
                "Anthropic",
                "aiProviderAnthropic",
                apiKey,
                baseUrl,
                timeoutMs,
                retryAttempts,
                retryBackoffMs,
                supportedModels,
                List.of("ANTHROPIC_API_KEY"),
                false
        );
        this.defaultMaxTokens = defaultMaxTokens;
        this.anthropicVersion = anthropicVersion;
    }

    AnthropicModelProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            String apiKey,
            String baseUrl,
            long timeoutMs,
            int retryAttempts,
            long retryBackoffMs,
            int defaultMaxTokens,
            List<String> supportedModels
    ) {
        this(objectMapper, circuitBreakerRegistry, AiPricingResolver.noop(), apiKey, baseUrl, timeoutMs, retryAttempts, retryBackoffMs, defaultMaxTokens, supportedModels, "2023-06-01");
    }

    @Override
    public AiPromptResponse sendPrompt(AiPromptRequest request) {
        return executePrompt(request, () -> doSendPrompt(request));
    }

    private AiPromptResponse doSendPrompt(AiPromptRequest request) {
        long startedAt = System.currentTimeMillis();
        String model = resolveModel(request);

        HttpRequest httpRequest = baseJsonRequest("/v1/messages", request)
                .header("x-api-key", apiKey)
                .header("anthropic-version", anthropicVersion)
                .POST(HttpRequest.BodyPublishers.ofString(writePayload(request, model)))
                .build();

        HttpResponse<String> response = send(httpRequest, request);
        if (response.statusCode() >= 400) {
            throw mapHttpError(request, response.statusCode(), response.body());
        }

        JsonNode root = readJson(response.body(), request, "messages");
        String content = readContent(root.path("content"));
        if (content.isBlank()) {
            throw mapHttpError(request, 502, "Resposta sem texto legivel");
        }

        JsonNode usageNode = root.path("usage");
        AiUsageEstimate usage = AiUsageEstimate.of(
                usageNode.path("input_tokens").asLong(0),
                usageNode.path("output_tokens").asLong(0)
        );

        return buildResponse(request, content, usage, startedAt, root.path("stop_reason").asText("completed"));
    }

    private String writePayload(AiPromptRequest request, String model) {
        try {
            return objectMapper.writeValueAsString(new AnthropicRequest(
                    model,
                    request.systemPrompt(),
                    request.prompt(),
                    request.temperature(),
                    request.maxTokens() == null ? defaultMaxTokens : request.maxTokens()
            ));
        } catch (Exception exception) {
            throw mapHttpError(request, 500, "Falha ao serializar payload da Anthropic");
        }
    }

    private String readContent(JsonNode contentNode) {
        if (!contentNode.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        contentNode.forEach(item -> {
            if ("text".equals(item.path("type").asText(""))) {
                builder.append(item.path("text").asText(""));
            }
        });
        return builder.toString().trim();
    }

    private record AnthropicRequest(
            String model,
            String system,
            Double temperature,
            int max_tokens,
            List<AnthropicMessage> messages
    ) {
        private AnthropicRequest(String model, String systemPrompt, String prompt, Double temperature, int maxTokens) {
            this(model, systemPrompt, temperature, maxTokens, List.of(new AnthropicMessage("user", prompt)));
        }
    }

    private record AnthropicMessage(String role, String content) {
    }
}
