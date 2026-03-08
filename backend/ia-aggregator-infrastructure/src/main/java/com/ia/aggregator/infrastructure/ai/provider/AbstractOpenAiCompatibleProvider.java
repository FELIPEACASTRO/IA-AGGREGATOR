package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.AiPromptRequest;
import com.ia.aggregator.application.ai.dto.AiPromptResponse;
import com.ia.aggregator.application.ai.dto.AiUsageEstimate;
import com.ia.aggregator.application.ai.port.out.AiPricingResolver;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

abstract class AbstractOpenAiCompatibleProvider extends AbstractAiProviderSupport {

    protected AbstractOpenAiCompatibleProvider(ObjectMapper objectMapper,
                                               CircuitBreakerRegistry circuitBreakerRegistry,
                                               AiPricingResolver pricingResolver,
                                               String providerId,
                                               String displayName,
                                               String circuitBreakerName,
                                               String apiKey,
                                               String baseUrl,
                                               long timeoutMs,
                                               int retryAttempts,
                                               long retryBackoffMs,
                                               List<String> supportedModels,
                                               List<String> requiredSecrets) {
        super(
                objectMapper,
                circuitBreakerRegistry,
                pricingResolver,
                providerId,
                displayName,
                circuitBreakerName,
                apiKey,
                baseUrl,
                timeoutMs,
                retryAttempts,
                retryBackoffMs,
                supportedModels,
                requiredSecrets,
                false
        );
    }

    @Override
    public AiPromptResponse sendPrompt(AiPromptRequest request) {
        return executePrompt(request, () -> doSendPrompt(request));
    }

    private AiPromptResponse doSendPrompt(AiPromptRequest request) {
        long startedAt = System.currentTimeMillis();
        String model = resolveModel(request);
        String payload = writeCompatiblePayload(request, model);

        HttpRequest httpRequest = baseJsonRequest("/chat/completions", request)
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = send(httpRequest, request);
        if (response.statusCode() >= 400) {
            throw mapHttpError(request, response.statusCode(), response.body());
        }

        JsonNode root = readJson(response.body(), request, "chat.completions");
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = readCompatibleContent(contentNode);
        if (content.isBlank()) {
            throw mapHttpError(request, 502, "Resposta vazia do provider " + providerName());
        }

        JsonNode usageNode = root.path("usage");
        AiUsageEstimate usage = new AiUsageEstimate(
                usageNode.path("prompt_tokens").asLong(0),
                usageNode.path("completion_tokens").asLong(0),
                usageNode.path("total_tokens").asLong(0)
        );
        String finishReason = root.path("choices").path(0).path("finish_reason").asText("completed");
        return buildResponse(request, content, usage, startedAt, finishReason);
    }

    protected String writeCompatiblePayload(AiPromptRequest request, String model) {
        try {
            return objectMapper.writeValueAsString(new CompatibleChatRequest(
                    model,
                    request.temperature(),
                    request.maxTokens(),
                    request.systemPrompt(),
                    request.prompt()
            ));
        } catch (Exception exception) {
            throw mapHttpError(request, 500, "Falha ao montar payload compatível");
        }
    }

    private String readCompatibleContent(JsonNode contentNode) {
        if (contentNode.isTextual()) {
            return contentNode.asText("").trim();
        }

        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            contentNode.forEach(entry -> {
                if (entry.path("type").asText("").equals("text")) {
                    builder.append(entry.path("text").asText(""));
                }
            });
            return builder.toString().trim();
        }

        return "";
    }

    private record CompatibleChatRequest(
            String model,
            Double temperature,
            Integer max_tokens,
            List<CompatibleChatMessage> messages
    ) {
        private CompatibleChatRequest(String model,
                                      Double temperature,
                                      Integer maxTokens,
                                      String systemPrompt,
                                      String prompt) {
            this(
                    model,
                    temperature == null ? 0.2d : temperature,
                    maxTokens,
                    systemPrompt == null || systemPrompt.isBlank()
                            ? List.of(new CompatibleChatMessage("user", prompt))
                            : List.of(
                                    new CompatibleChatMessage("system", systemPrompt),
                                    new CompatibleChatMessage("user", prompt)
                            )
            );
        }
    }

    private record CompatibleChatMessage(String role, String content) {
    }
}
