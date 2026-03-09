package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageGenerationCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractMediaProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Black Forest Labs (BFL) provider — FLUX image generation models.
 *
 * <p>API: {@code https://api.bfl.ml}
 * <p>Supports: IMAGE_GENERATION
 * <p>Auth: x-key header
 *
 * <p>Async: POST /v1/{model} -> GET /v1/get_result?id={id}
 * <p>Status mapping: "Ready" -> completed, "Error" -> failed, else processing.
 *
 * <p>Big O: O(P) where P = poll iterations, bounded by maxPollAttempts.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.bfl.api-key")
public class BflFluxProvider extends AbstractMediaProvider
        implements ImageGenerationCapable {

    private static final String PROVIDER_NAME = "bfl";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.IMAGE_GENERATION);

    private final ThreadLocal<String> currentModel = new ThreadLocal<>();

    public BflFluxProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.bfl.api-key:}") String apiKey,
            @Value("${app.ai.providers.bfl.base-url:https://api.bfl.ml}") String baseUrl,
            @Value("${app.ai.providers.bfl.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.bfl.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.bfl.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.bfl.supported-models:flux-pro-1.1,flux-dev,flux-pro}") List<String> supportedModels,
            @Value("${app.ai.providers.bfl.max-poll-attempts:40}") int maxPollAttempts,
            @Value("${app.ai.providers.bfl.poll-interval-ms:3000}") long pollIntervalMs
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderBfl"),
                new ApiKeyHeaderAuth("x-key", apiKey),
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
            if (request.width() != null) body.put("width", request.width());
            if (request.height() != null) body.put("height", request.height());

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
        return baseUrl + "/v1/" + model;
    }

    @Override
    protected String parseSubmitResponse(JsonNode responseRoot) {
        return responseRoot.path("id").asText();
    }

    @Override
    protected String pollJobEndpoint(String jobId) {
        return baseUrl + "/v1/get_result?id=" + jobId;
    }

    @Override
    protected AsyncJobStatus parsePollResponse(JsonNode responseRoot) {
        String status = responseRoot.path("status").asText("Pending");

        if ("Ready".equals(status)) {
            String resultUrl = responseRoot.path("result").path("sample").asText(null);
            return new AsyncJobStatus(null, "completed", resultUrl, null, 100, null);
        }

        if ("Error".equals(status)) {
            String error = responseRoot.path("result").asText("Generation failed");
            return new AsyncJobStatus(null, "failed", null, error, null, null);
        }

        return new AsyncJobStatus(null, "processing", null, null, null, null);
    }
}
