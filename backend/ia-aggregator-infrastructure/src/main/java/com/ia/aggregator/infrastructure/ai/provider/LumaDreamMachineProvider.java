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
 * Luma Dream Machine provider — image and video generation.
 *
 * <p>Supports: IMAGE_GENERATION, VIDEO_GENERATION
 * <p>API: {@code https://api.lumalabs.ai/dream-machine/v1}
 * <p>Async polling pattern.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.luma.api-key")
public class LumaDreamMachineProvider extends AbstractMediaProvider
        implements ImageGenerationCapable, VideoGenerationCapable {

    private static final String PROVIDER_NAME = "luma";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.IMAGE_GENERATION, Capability.VIDEO_GENERATION);

    private final ThreadLocal<String> currentEndpoint = new ThreadLocal<>();

    public LumaDreamMachineProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.luma.api-key:}") String apiKey,
            @Value("${app.ai.providers.luma.base-url:https://api.lumalabs.ai/dream-machine/v1}") String baseUrl,
            @Value("${app.ai.providers.luma.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.luma.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.luma.retry-backoff-ms:2000}") long retryBackoffMs,
            @Value("${app.ai.providers.luma.supported-models:dream-machine}") List<String> supportedModels,
            @Value("${app.ai.providers.luma.max-poll-attempts:60}") int maxPollAttempts,
            @Value("${app.ai.providers.luma.poll-interval-ms:5000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderLuma"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                supportedModels, maxPollAttempts, pollIntervalMs);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        currentEndpoint.set("/generations/image");
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", request.prompt());
            if (request.style() != null) body.put("style", request.style());
            AsyncJobStatus status = executeWithRetry(() -> submitAndPoll(body));
            return new ImageGenResult(
                    List.of(new ImageGenResult.GeneratedImage(status.resultUrl(), null, null)),
                    "dream-machine", PROVIDER_NAME);
        } finally { currentEndpoint.remove(); }
    }

    @Override
    public VideoGenResult generateVideo(VideoGenRequest request) {
        currentEndpoint.set("/generations");
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", request.prompt());
            if (request.imageUrl() != null) {
                body.put("keyframes", Map.of("frame0", Map.of("type", "image", "url", request.imageUrl())));
            }
            AsyncJobStatus status = executeWithRetry(() -> submitAndPoll(body));
            return new VideoGenResult(status.resultUrl(), "dream-machine", PROVIDER_NAME);
        } finally { currentEndpoint.remove(); }
    }

    @Override protected String submitJobEndpoint() { return baseUrl + currentEndpoint.get(); }
    @Override protected String parseSubmitResponse(JsonNode r) { return r.path("id").asText(); }
    @Override protected String pollJobEndpoint(String id) { return baseUrl + "/generations/" + id; }

    @Override
    protected AsyncJobStatus parsePollResponse(JsonNode r) {
        String state = r.path("state").asText("queued");
        if ("completed".equals(state)) {
            String url = r.path("assets").path("video").asText(
                    r.path("assets").path("image").asText(null));
            return new AsyncJobStatus(null, "completed", url, null, 100, null);
        }
        if ("failed".equals(state)) {
            return new AsyncJobStatus(null, "failed", null, r.path("failure_reason").asText("Unknown"), null, null);
        }
        return new AsyncJobStatus(null, "processing", null, null, null, null);
    }
}
