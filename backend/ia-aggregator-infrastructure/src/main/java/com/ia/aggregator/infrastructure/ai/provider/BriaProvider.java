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

@Component
@ConditionalOnProperty(name = "app.ai.providers.bria.api-key")
public class BriaProvider extends AbstractMediaProvider implements ImageGenerationCapable {

    private static final String PROVIDER_NAME = "bria";
    private static final Set<Capability> SUPPORTED = EnumSet.of(Capability.IMAGE_GENERATION);

    public BriaProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.bria.api-key:}") String apiKey,
            @Value("${app.ai.providers.bria.base-url:https://engine.prod.bria-api.com}") String baseUrl,
            @Value("${app.ai.providers.bria.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.bria.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.bria.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.bria.supported-models:bria-2.3,bria-2.2}") List<String> models
    ) {
        super(om, cbr.circuitBreaker("aiProviderBria"), new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, models, 1, 0);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED; }

    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", request.prompt());
            if (request.numberOfImages() != null) body.put("num_results", request.numberOfImages());
            String json = objectMapper.writeValueAsString(body);
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1/text-to-image/base/" + supportedModels.get(0)))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofMillis(timeoutMs));
            authStrategy.apply(req);
            var resp = httpClient.send(req.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400)
                throw new TechnicalException(ErrorCode.AI_010, providerName() + " HTTP " + resp.statusCode());
            JsonNode root = objectMapper.readTree(resp.body());
            List<ImageGenResult.GeneratedImage> images = new ArrayList<>();
            for (JsonNode img : root.path("result")) {
                images.add(new ImageGenResult.GeneratedImage(img.path("urls").path(0).asText(null), null, null));
            }
            return new ImageGenResult(images, supportedModels.get(0), PROVIDER_NAME);
        } catch (TechnicalException e) { throw e; }
        catch (Exception e) { throw new TechnicalException(ErrorCode.AI_010, providerName() + " failed: " + e.getMessage()); }
    }

    @Override protected String submitJobEndpoint() { return baseUrl + "/v1/text-to-image"; }
    @Override protected String parseSubmitResponse(JsonNode r) { return "sync"; }
    @Override protected String pollJobEndpoint(String id) { return baseUrl; }
    @Override protected AsyncJobStatus parsePollResponse(JsonNode r) {
        return new AsyncJobStatus(null, "completed", null, null, 100, null);
    }
}
