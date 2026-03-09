package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageGenerationCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractMediaProvider;
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

/**
 * Ideogram provider — high-quality image generation with typography.
 *
 * <p>API: {@code https://api.ideogram.ai}
 * <p>Supports: IMAGE_GENERATION
 * <p>Auth: Api-Key header
 *
 * <p>SYNCHRONOUS — no polling required. POST /generate returns result directly.
 *
 * <p>Big O: O(1) per request (single HTTP call, no polling loop).
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.ideogram.api-key")
public class IdeogramProvider extends AbstractMediaProvider
        implements ImageGenerationCapable {

    private static final String PROVIDER_NAME = "ideogram";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.IMAGE_GENERATION);

    public IdeogramProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.ideogram.api-key:}") String apiKey,
            @Value("${app.ai.providers.ideogram.base-url:https://api.ideogram.ai}") String baseUrl,
            @Value("${app.ai.providers.ideogram.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.ideogram.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.ideogram.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.ideogram.supported-models:V_2,V_2_TURBO}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderIdeogram"),
                new ApiKeyHeaderAuth("Api-Key", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                supportedModels, 1, 0);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<Capability> capabilities() {
        return SUPPORTED_CAPABILITIES;
    }

    /**
     * Synchronous image generation — overrides the async pattern.
     * POST /generate returns the image directly without polling.
     */
    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        String model = request.model() != null ? request.model() : supportedModels.get(0);

        Map<String, Object> imageRequest = new LinkedHashMap<>();
        imageRequest.put("prompt", request.prompt());
        imageRequest.put("model", model);
        if (request.width() != null && request.height() != null) {
            imageRequest.put("aspect_ratio", resolveAspectRatio(request.width(), request.height()));
        }
        if (request.style() != null) imageRequest.put("style_type", request.style());
        if (request.numberOfImages() != null) imageRequest.put("num_images", request.numberOfImages());

        Map<String, Object> body = Map.of("image_request", imageRequest);

        return executeWithRetry(() -> sendSynchronousRequest(body, model));
    }

    private ImageGenResult sendSynchronousRequest(Map<String, Object> body, String model) {
        try {
            String json = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/generate"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            authStrategy.apply(requestBuilder);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_010,
                        providerName() + " generation failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");

            List<ImageGenResult.GeneratedImage> images = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode item : data) {
                    images.add(new ImageGenResult.GeneratedImage(
                            item.path("url").asText(null),
                            null,
                            item.path("prompt").asText(null)
                    ));
                }
            }

            return new ImageGenResult(images, model, PROVIDER_NAME);
        } catch (TechnicalException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new TechnicalException(ErrorCode.AI_010,
                    "Failed to call " + providerName(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TechnicalException(ErrorCode.AI_010,
                    providerName() + " request interrupted", ex);
        }
    }

    private String resolveAspectRatio(int width, int height) {
        double ratio = (double) width / height;
        if (Math.abs(ratio - 1.0) < 0.05) return "ASPECT_1_1";
        if (Math.abs(ratio - 16.0 / 9.0) < 0.1) return "ASPECT_16_9";
        if (Math.abs(ratio - 9.0 / 16.0) < 0.1) return "ASPECT_9_16";
        if (Math.abs(ratio - 4.0 / 3.0) < 0.1) return "ASPECT_4_3";
        if (Math.abs(ratio - 3.0 / 4.0) < 0.1) return "ASPECT_3_4";
        return "ASPECT_1_1";
    }

    // Async template methods — not used for synchronous Ideogram, but required by abstract base.
    @Override
    protected String submitJobEndpoint() {
        return baseUrl + "/generate";
    }

    @Override
    protected String parseSubmitResponse(JsonNode responseRoot) {
        return "sync";
    }

    @Override
    protected String pollJobEndpoint(String jobId) {
        return baseUrl + "/generate";
    }

    @Override
    protected AsyncJobStatus parsePollResponse(JsonNode responseRoot) {
        return new AsyncJobStatus(null, "completed", null, null, 100, null);
    }
}
