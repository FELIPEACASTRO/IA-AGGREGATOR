package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.ChatRequest;
import com.ia.aggregator.application.ai.dto.ChatResult;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.AwsSigV4Auth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractNativeLlmProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AWS Bedrock provider for foundation models.
 *
 * <p>API: {@code https://bedrock-runtime.{region}.amazonaws.com/model/{modelId}/invoke}
 * <p>Supports: CHAT
 * <p>Auth: AWS SigV4 signing
 *
 * <p>Bedrock uses a converged API format across multiple foundation model providers.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.bedrock.access-key-id")
public class AwsBedrockProvider extends AbstractNativeLlmProvider {

    private static final String PROVIDER_NAME = "bedrock";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.CHAT);

    private final String region;

    public AwsBedrockProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.bedrock.access-key-id:}") String accessKeyId,
            @Value("${app.ai.providers.bedrock.secret-access-key:}") String secretAccessKey,
            @Value("${app.ai.providers.bedrock.region:us-east-1}") String region,
            @Value("${app.ai.providers.bedrock.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.bedrock.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.bedrock.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.bedrock.supported-models:anthropic.claude-3-5-sonnet-20241022-v2:0,amazon.nova-pro-v1:0}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderBedrock"),
                new AwsSigV4Auth(accessKeyId, secretAccessKey, region, "bedrock"),
                "https://bedrock-runtime." + region + ".amazonaws.com",
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
    protected Object buildChatRequestBody(ChatRequest request, String model) {
        // Bedrock Converse API format
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("modelId", model);
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", List.of(Map.of("text", request.prompt())));
        messages.add(userMessage);
        body.put("messages", messages);

        if (request.systemPrompt() != null) {
            body.put("system", List.of(Map.of("text", request.systemPrompt())));
        }

        Map<String, Object> inferenceConfig = new LinkedHashMap<>();
        if (request.maxTokens() != null) inferenceConfig.put("maxTokens", request.maxTokens());
        if (request.temperature() != null) inferenceConfig.put("temperature", request.temperature());
        if (!inferenceConfig.isEmpty()) body.put("inferenceConfig", inferenceConfig);

        return body;
    }

    @Override
    protected ChatResult parseChatResponse(JsonNode root, String model) {
        // Bedrock Converse API response
        String content = root.path("output").path("message").path("content")
                .path(0).path("text").asText("");
        String stopReason = root.path("stopReason").asText(null);
        Integer inputTokens = root.has("usage") ? root.path("usage").path("inputTokens").asInt(0) : null;
        Integer outputTokens = root.has("usage") ? root.path("usage").path("outputTokens").asInt(0) : null;
        return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                inputTokens, outputTokens, stopReason);
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl + "/model/" + model + "/converse";
    }
}
