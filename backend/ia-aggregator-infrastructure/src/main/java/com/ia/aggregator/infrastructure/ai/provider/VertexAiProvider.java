package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.OAuth2ClientCredentialsAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractNativeLlmProvider;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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
import java.util.function.Supplier;

/**
 * Google Vertex AI provider -- enterprise Gemini models via Vertex AI platform.
 *
 * <p>API: {@code https://{region}-aiplatform.googleapis.com}
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>Auth: OAuth2 Client Credentials (Google service account)
 *
 * <p>POST /v1/projects/{projectId}/locations/{region}/publishers/google/models/{model}:generateContent
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.vertex-ai.client-id")
public class VertexAiProvider extends AbstractNativeLlmProvider
        implements EmbeddingCapable {

    private static final String PROVIDER_NAME = "vertex-ai";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    private final String region;
    private final String projectId;

    public VertexAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.vertex-ai.client-id:}") String clientId,
            @Value("${app.ai.providers.vertex-ai.client-secret:}") String clientSecret,
            @Value("${app.ai.providers.vertex-ai.region:us-central1}") String region,
            @Value("${app.ai.providers.vertex-ai.project-id:}") String projectId,
            @Value("${app.ai.providers.vertex-ai.base-url:}") String baseUrlOverride,
            @Value("${app.ai.providers.vertex-ai.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.vertex-ai.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.vertex-ai.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.vertex-ai.supported-models:gemini-1.5-pro,gemini-1.5-flash}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderVertexAi"),
                new OAuth2ClientCredentialsAuth(
                        "https://oauth2.googleapis.com/token",
                        clientId, clientSecret,
                        "https://www.googleapis.com/auth/cloud-platform"
                ),
                baseUrlOverride != null && !baseUrlOverride.isBlank()
                        ? baseUrlOverride
                        : "https://" + region + "-aiplatform.googleapis.com",
                timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
        this.region = region;
        this.projectId = projectId;
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    protected Object buildChatRequestBody(ChatRequest request, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.systemPrompt() != null) {
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", request.systemPrompt()))
            ));
        }
        body.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", request.prompt())))
        ));

        Map<String, Object> genConfig = new LinkedHashMap<>();
        if (request.temperature() != null) genConfig.put("temperature", request.temperature());
        if (request.maxTokens() != null) genConfig.put("maxOutputTokens", request.maxTokens());
        if (!genConfig.isEmpty()) body.put("generationConfig", genConfig);

        return body;
    }

    @Override
    protected ChatResult parseChatResponse(JsonNode responseRoot, String model) {
        String content = responseRoot.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text").asText(null);

        if (content == null || content.isBlank()) {
            throw new TechnicalException(ErrorCode.AI_005,
                    "Vertex AI returned empty response content");
        }

        Integer promptTokens = responseRoot.path("usageMetadata").has("promptTokenCount")
                ? responseRoot.path("usageMetadata").path("promptTokenCount").asInt() : null;
        Integer completionTokens = responseRoot.path("usageMetadata").has("candidatesTokenCount")
                ? responseRoot.path("usageMetadata").path("candidatesTokenCount").asInt() : null;

        return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                promptTokens, completionTokens, "stop");
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl + "/v1/projects/" + projectId
                + "/locations/" + region
                + "/publishers/google/models/" + model + ":generateContent";
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        String model = request.model() != null ? request.model() : "text-embedding-004";
        Supplier<EmbeddingResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callEmbed(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002,
                    "Vertex AI circuit breaker is open", ex);
        }
    }

    private EmbeddingResult callEmbed(EmbeddingRequest request, String model) {
        try {
            List<Map<String, Object>> instances = new ArrayList<>();
            for (String text : request.input()) {
                instances.add(Map.of("content", text));
            }
            Map<String, Object> body = Map.of("instances", instances);

            String endpoint = baseUrl + "/v1/projects/" + projectId
                    + "/locations/" + region
                    + "/publishers/google/models/" + model + ":predict";

            String json = objectMapper.writeValueAsString(body);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            authStrategy.apply(reqBuilder);
            HttpResponse<String> response = httpClient.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_008,
                        "Vertex AI embedding failed: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<float[]> embeddings = new ArrayList<>();
            int dimensions = 0;
            JsonNode predictions = root.path("predictions");
            if (predictions.isArray()) {
                for (JsonNode pred : predictions) {
                    JsonNode values = pred.path("embeddings").path("values");
                    float[] vector = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        vector[i] = (float) values.get(i).asDouble();
                    }
                    embeddings.add(vector);
                    if (dimensions == 0) dimensions = vector.length;
                }
            }

            return new EmbeddingResult(embeddings, model, PROVIDER_NAME, dimensions, null);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_005,
                    "Failed to communicate with Vertex AI", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002,
                    "Vertex AI embedding request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_008,
                    "Vertex AI embedding request failed", ex);
        }
    }
}
