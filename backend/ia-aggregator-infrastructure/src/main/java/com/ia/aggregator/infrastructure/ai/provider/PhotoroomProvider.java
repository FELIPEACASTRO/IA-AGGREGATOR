package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageEditCapable;
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
@ConditionalOnProperty(name = "app.ai.providers.photoroom.api-key")
public class PhotoroomProvider extends AbstractMediaProvider implements ImageEditCapable {

    private static final String PROVIDER_NAME = "photoroom";
    private static final Set<Capability> SUPPORTED = EnumSet.of(Capability.IMAGE_EDITING);

    public PhotoroomProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.photoroom.api-key:}") String apiKey,
            @Value("${app.ai.providers.photoroom.base-url:https://sdk.photoroom.com}") String baseUrl,
            @Value("${app.ai.providers.photoroom.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.photoroom.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.photoroom.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.photoroom.supported-models:photoroom-v1}") List<String> models
    ) {
        super(om, cbr.circuitBreaker("aiProviderPhotoroom"),
                new ApiKeyHeaderAuth("x-api-key", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, models, 1, 0);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED; }

    @Override
    public ImageGenResult editImage(ImageEditRequest request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("imageUrl", request.imageData());
            if (request.prompt() != null) body.put("prompt", request.prompt());
            String json = objectMapper.writeValueAsString(body);
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1/segment"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofMillis(timeoutMs));
            authStrategy.apply(req);
            var resp = httpClient.send(req.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400)
                throw new TechnicalException(ErrorCode.AI_010, providerName() + " HTTP " + resp.statusCode());
            JsonNode root = objectMapper.readTree(resp.body());
            String resultUrl = root.path("result_url").asText(null);
            return new ImageGenResult(
                    List.of(new ImageGenResult.GeneratedImage(resultUrl, null, null)),
                    "photoroom-v1", PROVIDER_NAME);
        } catch (TechnicalException e) { throw e; }
        catch (Exception e) { throw new TechnicalException(ErrorCode.AI_010, providerName() + " failed: " + e.getMessage()); }
    }

    @Override protected String submitJobEndpoint() { return baseUrl + "/v1/segment"; }
    @Override protected String parseSubmitResponse(JsonNode r) { return "sync"; }
    @Override protected String pollJobEndpoint(String id) { return baseUrl; }
    @Override protected AsyncJobStatus parsePollResponse(JsonNode r) {
        return new AsyncJobStatus(null, "completed", r.path("result_url").asText(null), null, 100, null);
    }
}
