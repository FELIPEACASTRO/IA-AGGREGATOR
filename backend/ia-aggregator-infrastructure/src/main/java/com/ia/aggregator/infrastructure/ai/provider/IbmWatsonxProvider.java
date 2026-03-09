package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.IbmIamAuthStrategy;
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
 * IBM watsonx.ai provider -- enterprise AI foundation models on IBM Cloud.
 *
 * <p>API: {@code https://{region}.ml.cloud.ibm.com}
 * <p>Supports: CHAT, EMBEDDINGS
 * <p>Auth: IBM IAM token exchange (API key -> bearer token)
 *
 * <p>POST /ml/v1/text/generation?version=2024-01-01
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.ibm-watsonx.api-key")
public class IbmWatsonxProvider extends AbstractNativeLlmProvider
        implements EmbeddingCapable {

    private static final String PROVIDER_NAME = "ibm-watsonx";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT, Capability.EMBEDDINGS);

    private final String projectId;

    public IbmWatsonxProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.ibm-watsonx.api-key:}") String apiKey,
            @Value("${app.ai.providers.ibm-watsonx.region:us-south}") String region,
            @Value("${app.ai.providers.ibm-watsonx.project-id:}") String projectId,
            @Value("${app.ai.providers.ibm-watsonx.base-url:}") String baseUrlOverride,
            @Value("${app.ai.providers.ibm-watsonx.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.ibm-watsonx.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.ibm-watsonx.retry-backoff-ms:500}") long retryBackoffMs,
            @Value("${app.ai.providers.ibm-watsonx.supported-models:ibm/granite-3-8b-instruct,meta-llama/llama-3-70b-instruct}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderIbmWatsonx"),
                new IbmIamAuthStrategy(apiKey),
                baseUrlOverride != null && !baseUrlOverride.isBlank()
                        ? baseUrlOverride
                        : "https://" + region + ".ml.cloud.ibm.com",
                timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
        this.projectId = projectId;
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    protected Object buildChatRequestBody(ChatRequest request, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model_id", model);
        body.put("input", request.prompt());
        body.put("project_id", projectId);

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("max_new_tokens", request.maxTokens() != null ? request.maxTokens() : 800);
        if (request.temperature() != null) parameters.put("temperature", request.temperature());
        body.put("parameters", parameters);

        return body;
    }

    @Override
    protected ChatResult parseChatResponse(JsonNode responseRoot, String model) {
        String content = responseRoot.path("results").path(0)
                .path("generated_text").asText(null);

        if (content == null || content.isBlank()) {
            throw new TechnicalException(ErrorCode.AI_005,
                    "IBM watsonx returned empty response content");
        }

        Integer inputTokens = responseRoot.path("results").path(0).has("input_token_count")
                ? responseRoot.path("results").path(0).path("input_token_count").asInt() : null;
        Integer outputTokens = responseRoot.path("results").path(0).has("generated_token_count")
                ? responseRoot.path("results").path(0).path("generated_token_count").asInt() : null;
        String stopReason = responseRoot.path("results").path(0)
                .path("stop_reason").asText(null);

        return new ChatResult(content, model, PROVIDER_NAME, false, 1,
                inputTokens, outputTokens, stopReason);
    }

    @Override
    protected String resolveEndpointUrl(String model) {
        return baseUrl + "/ml/v1/text/generation?version=2024-01-01";
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        String model = request.model() != null ? request.model() : "ibm/slate-30m-english-rtrvr";
        Supplier<EmbeddingResult> guardedCall = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> callEmbed(request, model));
        try {
            return executeWithRetry(guardedCall);
        } catch (CallNotPermittedException ex) {
            throw new TechnicalException(ErrorCode.AI_002,
                    "IBM watsonx circuit breaker is open", ex);
        }
    }

    private EmbeddingResult callEmbed(EmbeddingRequest request, String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model_id", model);
            body.put("inputs", request.input());
            body.put("project_id", projectId);

            String endpoint = baseUrl + "/ml/v1/text/embeddings?version=2024-01-01";
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
                        "IBM watsonx embedding failed: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<float[]> embeddings = new ArrayList<>();
            int dimensions = 0;
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode result : results) {
                    JsonNode embedding = result.path("embedding");
                    float[] vector = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        vector[i] = (float) embedding.get(i).asDouble();
                    }
                    embeddings.add(vector);
                    if (dimensions == 0) dimensions = vector.length;
                }
            }

            Integer totalTokens = root.path("results").path(0).has("input_token_count")
                    ? root.path("results").path(0).path("input_token_count").asInt() : null;

            return new EmbeddingResult(embeddings, model, PROVIDER_NAME, dimensions, totalTokens);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_005,
                    "Failed to communicate with IBM watsonx", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_002,
                    "IBM watsonx embedding request interrupted", ex);
        } catch (Exception ex) {
            throw new TechnicalException(ErrorCode.AI_008,
                    "IBM watsonx embedding request failed", ex);
        }
    }
}
