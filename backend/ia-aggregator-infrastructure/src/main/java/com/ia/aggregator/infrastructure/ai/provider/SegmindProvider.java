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

import java.util.*;

@Component
@ConditionalOnProperty(name = "app.ai.providers.segmind.api-key")
public class SegmindProvider extends AbstractMediaProvider implements ImageGenerationCapable {

    private static final String PROVIDER_NAME = "segmind";
    private static final Set<Capability> SUPPORTED = EnumSet.of(Capability.IMAGE_GENERATION);

    public SegmindProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.segmind.api-key:}") String apiKey,
            @Value("${app.ai.providers.segmind.base-url:https://api.segmind.com}") String baseUrl,
            @Value("${app.ai.providers.segmind.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.segmind.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.segmind.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.segmind.supported-models:sdxl1.0-txt2img,flux-schnell}") List<String> models
    ) {
        super(om, cbr.circuitBreaker("aiProviderSegmind"),
                new ApiKeyHeaderAuth("x-api-key", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, models, 1, 0);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED; }

    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        try {
            String model = request.model() != null ? request.model() : supportedModels.get(0);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", request.prompt());
            if (request.width() != null) body.put("img_width", request.width());
            if (request.height() != null) body.put("img_height", request.height());
            String json = objectMapper.writeValueAsString(body);
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1/" + model))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofMillis(timeoutMs));
            authStrategy.apply(req);
            var resp = httpClient.send(req.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400)
                throw new TechnicalException(ErrorCode.AI_010, providerName() + " HTTP " + resp.statusCode());
            JsonNode root = objectMapper.readTree(resp.body());
            String imageUrl = root.path("image").asText(root.path("output_url").asText(null));
            return new ImageGenResult(
                    List.of(new ImageGenResult.GeneratedImage(imageUrl, null, null)),
                    model, PROVIDER_NAME);
        } catch (TechnicalException e) { throw e; }
        catch (Exception e) { throw new TechnicalException(ErrorCode.AI_010, providerName() + " failed: " + e.getMessage()); }
    }

    @Override protected String submitJobEndpoint() { return baseUrl + "/v1/" + supportedModels.get(0); }
    @Override protected String parseSubmitResponse(JsonNode r) { return "sync"; }
    @Override protected String pollJobEndpoint(String id) { return baseUrl; }
    @Override protected AsyncJobStatus parsePollResponse(JsonNode r) {
        return new AsyncJobStatus(null, "completed", r.path("image").asText(null), null, 100, null);
    }
}
