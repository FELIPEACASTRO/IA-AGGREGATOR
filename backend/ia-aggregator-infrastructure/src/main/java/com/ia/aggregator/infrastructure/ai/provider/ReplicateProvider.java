package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageGenerationCapable;
import com.ia.aggregator.application.ai.port.out.capability.VideoGenerationCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractMediaProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Replicate provider — marketplace for open-source models.
 *
 * <p>API: {@code https://api.replicate.com}
 * <p>Supports: IMAGE_GENERATION, VIDEO_GENERATION
 * <p>Auth: Bearer token
 *
 * <p>Async: POST /v1/predictions -> GET /v1/predictions/{id}
 * <p>Status mapping: "succeeded" -> completed, "failed"/"canceled" -> failed, else processing.
 *
 * <p>Big O: O(P) where P = poll iterations, bounded by maxPollAttempts.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.replicate.api-key")
public class ReplicateProvider extends AbstractMediaProvider
        implements ImageGenerationCapable, VideoGenerationCapable {

    private static final String PROVIDER_NAME = "replicate";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.IMAGE_GENERATION, Capability.VIDEO_GENERATION);

    public ReplicateProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.replicate.api-key:}") String apiKey,
            @Value("${app.ai.providers.replicate.base-url:https://api.replicate.com}") String baseUrl,
            @Value("${app.ai.providers.replicate.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.replicate.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.replicate.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.replicate.supported-models:stability-ai/sdxl,black-forest-labs/flux-schnell,minimax/video-01}") List<String> supportedModels,
            @Value("${app.ai.providers.replicate.max-poll-attempts:60}") int maxPollAttempts,
            @Value("${app.ai.providers.replicate.poll-interval-ms:3000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderReplicate"),
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

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", request.prompt());
        if (request.width() != null) input.put("width", request.width());
        if (request.height() != null) input.put("height", request.height());
        if (request.numberOfImages() != null) input.put("num_outputs", request.numberOfImages());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", model);
        body.put("input", input);

        AsyncJobStatus jobStatus = executeWithRetry(() -> submitAndPoll(body));

        return new ImageGenResult(
                List.of(new ImageGenResult.GeneratedImage(jobStatus.resultUrl(), null, null)),
                model,
                PROVIDER_NAME
        );
    }

    @Override
    public VideoGenResult generateVideo(VideoGenRequest request) {
        String model = request.model() != null ? request.model() : "minimax/video-01";

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("prompt", request.prompt());
        if (request.imageUrl() != null) input.put("image", request.imageUrl());
        if (request.durationSeconds() != null) input.put("duration", request.durationSeconds());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", model);
        body.put("input", input);

        AsyncJobStatus jobStatus = executeWithRetry(() -> submitAndPoll(body));

        return new VideoGenResult(jobStatus.resultUrl(), model, PROVIDER_NAME);
    }

    @Override
    protected String submitJobEndpoint() {
        return baseUrl + "/v1/predictions";
    }

    @Override
    protected String parseSubmitResponse(JsonNode responseRoot) {
        return responseRoot.path("id").asText();
    }

    @Override
    protected String pollJobEndpoint(String jobId) {
        return baseUrl + "/v1/predictions/" + jobId;
    }

    @Override
    protected AsyncJobStatus parsePollResponse(JsonNode responseRoot) {
        String status = responseRoot.path("status").asText("starting");

        if ("succeeded".equals(status)) {
            JsonNode output = responseRoot.path("output");
            String resultUrl = output.isArray() && output.size() > 0
                    ? output.get(0).asText()
                    : output.asText(null);
            return new AsyncJobStatus(null, "completed", resultUrl, null, 100, null);
        }

        if ("failed".equals(status) || "canceled".equals(status)) {
            String error = responseRoot.path("error").asText("Job " + status);
            return new AsyncJobStatus(null, "failed", null, error, null, null);
        }

        return new AsyncJobStatus(null, "processing", null, null, null, null);
    }
}
