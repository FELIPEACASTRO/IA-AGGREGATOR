package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageEditCapable;
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
 * Stability AI provider (Stable Diffusion, SDXL, SD3, etc.).
 *
 * <p>API: {@code https://api.stability.ai/v2beta}
 * <p>Supports: IMAGE_GENERATION, IMAGE_EDITING
 * <p>Auth: Bearer token
 *
 * <p>Uses async polling: POST submit → GET /v2beta/results/{id}
 * <p>Polling timeout: 120s (default maxPollAttempts=40, pollInterval=3000ms)
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.stability.api-key")
public class StabilityAiProvider extends AbstractMediaProvider
        implements ImageGenerationCapable, ImageEditCapable {

    private static final String PROVIDER_NAME = "stability";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.IMAGE_GENERATION, Capability.IMAGE_EDITING);

    private final ThreadLocal<String> currentModel = new ThreadLocal<>();

    public StabilityAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.stability.api-key:}") String apiKey,
            @Value("${app.ai.providers.stability.base-url:https://api.stability.ai}") String baseUrl,
            @Value("${app.ai.providers.stability.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.stability.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.stability.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.stability.supported-models:sd3-large,sd3-medium,stable-image-ultra}") List<String> supportedModels,
            @Value("${app.ai.providers.stability.max-poll-attempts:40}") int maxPollAttempts,
            @Value("${app.ai.providers.stability.poll-interval-ms:3000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderStability"),
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
        currentModel.set(model);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", request.prompt());
            body.put("model", model);
            if (request.width() != null) body.put("width", request.width());
            if (request.height() != null) body.put("height", request.height());
            if (request.style() != null) body.put("style_preset", request.style());
            if (request.numberOfImages() != null) body.put("samples", request.numberOfImages());
            body.put("output_format", "png");

            AsyncJobStatus jobStatus = executeWithRetry(() -> submitAndPoll(body));

            return new ImageGenResult(
                    List.of(new ImageGenResult.GeneratedImage(jobStatus.resultUrl(), null, null)),
                    model,
                    PROVIDER_NAME
            );
        } finally {
            currentModel.remove();
        }
    }

    @Override
    public ImageGenResult editImage(ImageEditRequest request) {
        String model = request.model() != null ? request.model() : "sd3-large";
        currentModel.set(model);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("image", request.imageData());
            body.put("prompt", request.prompt());
            body.put("model", model);
            if (request.maskData() != null) body.put("mask", request.maskData());
            body.put("output_format", "png");

            AsyncJobStatus jobStatus = executeWithRetry(() -> submitAndPoll(body));

            return new ImageGenResult(
                    List.of(new ImageGenResult.GeneratedImage(jobStatus.resultUrl(), null, null)),
                    model,
                    PROVIDER_NAME
            );
        } finally {
            currentModel.remove();
        }
    }

    @Override
    protected String submitJobEndpoint() {
        String model = currentModel.get() != null ? currentModel.get() : supportedModels.get(0);
        return baseUrl + "/v2beta/stable-image/generate/" + model;
    }

    @Override
    protected String parseSubmitResponse(JsonNode responseRoot) {
        return responseRoot.path("id").asText();
    }

    @Override
    protected String pollJobEndpoint(String jobId) {
        return baseUrl + "/v2beta/results/" + jobId;
    }

    @Override
    protected AsyncJobStatus parsePollResponse(JsonNode responseRoot) {
        String status = responseRoot.path("status").asText("processing");

        if ("complete".equals(status) || "succeeded".equals(status)) {
            String resultUrl = responseRoot.path("output").path(0).path("url").asText(null);
            if (resultUrl == null) {
                resultUrl = responseRoot.path("result").asText(null);
            }
            return new AsyncJobStatus(null, "completed", resultUrl, null, 100, null);
        }

        if ("failed".equals(status)) {
            String error = responseRoot.path("errors").path(0).asText("Unknown error");
            return new AsyncJobStatus(null, "failed", null, error, null, null);
        }

        return new AsyncJobStatus(null, "processing", null, null, null, null);
    }
}
