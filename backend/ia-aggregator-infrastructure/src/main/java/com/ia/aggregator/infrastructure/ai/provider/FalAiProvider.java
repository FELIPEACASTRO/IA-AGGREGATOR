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
 * fal.ai provider (FLUX, Kling, Minimax models).
 *
 * <p>API: {@code https://queue.fal.run/{model}}
 * <p>Supports: IMAGE_GENERATION, VIDEO_GENERATION
 * <p>Auth: Bearer token (fal Key)
 *
 * <p>Uses async queue: POST submit → GET /queue/requests/{id}/status
 * <p>Polling timeout: 300s for video (maxPollAttempts=60, pollInterval=5000ms)
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.fal.api-key")
public class FalAiProvider extends AbstractMediaProvider
        implements ImageGenerationCapable, VideoGenerationCapable {

    private static final String PROVIDER_NAME = "fal";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.IMAGE_GENERATION, Capability.VIDEO_GENERATION);

    private final ThreadLocal<String> currentModel = new ThreadLocal<>();

    public FalAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.fal.api-key:}") String apiKey,
            @Value("${app.ai.providers.fal.base-url:https://queue.fal.run}") String baseUrl,
            @Value("${app.ai.providers.fal.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.fal.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.fal.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.fal.supported-models:fal-ai/flux/dev,fal-ai/flux/schnell,fal-ai/kling-video/v1}") List<String> supportedModels,
            @Value("${app.ai.providers.fal.max-poll-attempts:60}") int maxPollAttempts,
            @Value("${app.ai.providers.fal.poll-interval-ms:5000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderFal"),
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
        String model = request.model() != null ? request.model() : "fal-ai/flux/dev";
        currentModel.set(model);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", request.prompt());
            if (request.width() != null) body.put("image_size", Map.of("width", request.width(), "height",
                    request.height() != null ? request.height() : request.width()));
            if (request.numberOfImages() != null) body.put("num_images", request.numberOfImages());

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
    public VideoGenResult generateVideo(VideoGenRequest request) {
        String model = request.model() != null ? request.model() : "fal-ai/kling-video/v1";
        currentModel.set(model);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", request.prompt());
            if (request.imageUrl() != null) body.put("image_url", request.imageUrl());
            if (request.durationSeconds() != null) body.put("duration", String.valueOf(request.durationSeconds()));
            if (request.aspectRatio() != null) body.put("aspect_ratio", request.aspectRatio());

            AsyncJobStatus jobStatus = executeWithRetry(() -> submitAndPoll(body));

            return new VideoGenResult(jobStatus.resultUrl(), model, PROVIDER_NAME);
        } finally {
            currentModel.remove();
        }
    }

    @Override
    protected String submitJobEndpoint() {
        return baseUrl + "/" + currentModel.get();
    }

    @Override
    protected String parseSubmitResponse(JsonNode responseRoot) {
        return responseRoot.path("request_id").asText();
    }

    @Override
    protected String pollJobEndpoint(String jobId) {
        return baseUrl + "/" + currentModel.get() + "/requests/" + jobId + "/status";
    }

    @Override
    protected AsyncJobStatus parsePollResponse(JsonNode responseRoot) {
        String status = responseRoot.path("status").asText("IN_QUEUE");

        if ("COMPLETED".equals(status)) {
            String resultUrl = responseRoot.path("response_url").asText(null);
            return new AsyncJobStatus(null, "completed", resultUrl, null, 100, null);
        }

        if ("FAILED".equals(status)) {
            String error = responseRoot.path("error").asText("Unknown error");
            return new AsyncJobStatus(null, "failed", null, error, null, null);
        }

        // IN_QUEUE or IN_PROGRESS
        int progress = responseRoot.path("queue_position").asInt(0);
        return new AsyncJobStatus(null, "processing", null, null, progress, null);
    }
}
