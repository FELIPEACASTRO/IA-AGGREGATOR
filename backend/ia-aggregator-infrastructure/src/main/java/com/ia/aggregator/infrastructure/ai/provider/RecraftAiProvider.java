package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageGenerationCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractMediaProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Recraft AI provider — vector and raster image generation.
 *
 * <p>Supports: IMAGE_GENERATION
 * <p>API: {@code https://external.api.recraft.ai/v1}
 * <p>Sync API — uses AbstractMediaProvider with single-attempt polling.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.recraft.api-key")
public class RecraftAiProvider extends AbstractMediaProvider implements ImageGenerationCapable {

    private static final String PROVIDER_NAME = "recraft";
    private static final Set<Capability> SUPPORTED_CAPABILITIES = EnumSet.of(Capability.IMAGE_GENERATION);

    public RecraftAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.recraft.api-key:}") String apiKey,
            @Value("${app.ai.providers.recraft.base-url:https://external.api.recraft.ai}") String baseUrl,
            @Value("${app.ai.providers.recraft.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.recraft.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.recraft.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.recraft.supported-models:recraftv3,recraft20b}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderRecraft"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                supportedModels, 1, 0);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        try {
            String model = request.model() != null ? request.model() : supportedModels.get(0);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", request.prompt());
            body.put("model", model);
            if (request.style() != null) body.put("style", request.style());
            if (request.width() != null && request.height() != null) {
                body.put("size", request.width() + "x" + request.height());
            }
            if (request.numberOfImages() != null) body.put("n", request.numberOfImages());

            String json = objectMapper.writeValueAsString(body);
            var reqBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1/images/generations"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofMillis(timeoutMs));
            authStrategy.apply(reqBuilder);

            var response = httpClient.send(reqBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new TechnicalException(ErrorCode.AI_010, providerName() + " HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            List<ImageGenResult.GeneratedImage> images = new ArrayList<>();
            for (JsonNode img : root.path("data")) {
                images.add(new ImageGenResult.GeneratedImage(img.path("url").asText(null), null, null));
            }
            return new ImageGenResult(images, model, PROVIDER_NAME);
        } catch (TechnicalException e) { throw e; }
        catch (Exception e) {
            throw new TechnicalException(ErrorCode.AI_010, providerName() + " image gen failed: " + e.getMessage());
        }
    }

    @Override protected String submitJobEndpoint() { return baseUrl + "/v1/images/generations"; }
    @Override protected String parseSubmitResponse(JsonNode r) { return r.path("id").asText(); }
    @Override protected String pollJobEndpoint(String id) { return baseUrl + "/v1/images/" + id; }
    @Override protected AsyncJobStatus parsePollResponse(JsonNode r) {
        return new AsyncJobStatus(null, "completed", r.path("data").path(0).path("url").asText(null), null, 100, null);
    }
}
