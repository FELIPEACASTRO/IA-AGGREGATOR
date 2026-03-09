package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageGenerationCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractMediaProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Leonardo.AI provider — AI image generation with creative control.
 *
 * <p>API: {@code https://cloud.leonardo.ai/api/rest/v1}
 * <p>Supports: IMAGE_GENERATION
 * <p>Auth: Bearer token
 *
 * <p>Async: POST /generations -> parse generationId -> GET /generations/{id}
 * <p>Status mapping: "COMPLETE" -> completed, "FAILED" -> failed, else processing.
 *
 * <p>Big O: O(P) where P = poll iterations, bounded by maxPollAttempts.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.leonardo.api-key")
public class LeonardoAiProvider extends AbstractMediaProvider
        implements ImageGenerationCapable {

    private static final String PROVIDER_NAME = "leonardo";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.IMAGE_GENERATION);

    public LeonardoAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.leonardo.api-key:}") String apiKey,
            @Value("${app.ai.providers.leonardo.base-url:https://cloud.leonardo.ai/api/rest/v1}") String baseUrl,
            @Value("${app.ai.providers.leonardo.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.leonardo.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.leonardo.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.leonardo.supported-models:6b645e3a-d64f-4341-a6d8-7a3690fbf042,aa77f04e-83f0-47d0-9b96-9b97b4c3b7d4}") List<String> supportedModels,
            @Value("${app.ai.providers.leonardo.max-poll-attempts:40}") int maxPollAttempts,
            @Value("${app.ai.providers.leonardo.poll-interval-ms:3000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderLeonardo"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                supportedModels, maxPollAttempts, pollIntervalMs);
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
    public ImageGenResult generateImage(ImageGenRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", request.prompt());
        body.put("modelId", model);
        if (request.width() != null) body.put("width", request.width());
        if (request.height() != null) body.put("height", request.height());
        if (request.numberOfImages() != null) body.put("num_images", request.numberOfImages());
        if (request.style() != null) body.put("presetStyle", request.style());

        AsyncJobStatus jobStatus = executeWithRetry(() -> submitAndPoll(body));

        return new ImageGenResult(
                List.of(new ImageGenResult.GeneratedImage(jobStatus.resultUrl(), null, null)),
                model,
                PROVIDER_NAME
        );
    }

    @Override
    protected String submitJobEndpoint() {
        return baseUrl + "/generations";
    }

    @Override
    protected String parseSubmitResponse(JsonNode responseRoot) {
        return responseRoot
                .path("sdGenerationJob")
                .path("generationId")
                .asText();
    }

    @Override
    protected String pollJobEndpoint(String jobId) {
        return baseUrl + "/generations/" + jobId;
    }

    @Override
    protected AsyncJobStatus parsePollResponse(JsonNode responseRoot) {
        String status = responseRoot
                .path("generations_by_pk")
                .path("status")
                .asText("PENDING");

        if ("COMPLETE".equals(status)) {
            JsonNode images = responseRoot
                    .path("generations_by_pk")
                    .path("generated_images");
            String resultUrl = null;
            if (images.isArray() && images.size() > 0) {
                resultUrl = images.get(0).path("url").asText(null);
            }
            return new AsyncJobStatus(null, "completed", resultUrl, null, 100, null);
        }

        if ("FAILED".equals(status)) {
            return new AsyncJobStatus(null, "failed", null, "Leonardo generation failed", null, null);
        }

        return new AsyncJobStatus(null, "processing", null, null, null, null);
    }
}
