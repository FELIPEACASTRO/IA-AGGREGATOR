package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageEditCapable;
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
@ConditionalOnProperty(name = "app.ai.providers.getimg.api-key")
public class GetimgAiProvider extends AbstractMediaProvider
        implements ImageGenerationCapable, ImageEditCapable {

    private static final String PROVIDER_NAME = "getimg";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.IMAGE_GENERATION, Capability.IMAGE_EDITING);

    public GetimgAiProvider(
            ObjectMapper objectMapper, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.getimg.api-key:}") String apiKey,
            @Value("${app.ai.providers.getimg.base-url:https://api.getimg.ai}") String baseUrl,
            @Value("${app.ai.providers.getimg.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.getimg.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.getimg.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.getimg.supported-models:stable-diffusion-xl-v1-0,essential-v2}") List<String> supportedModels
    ) {
        super(objectMapper, cbr.circuitBreaker("aiProviderGetimg"),
                new BearerTokenAuth(apiKey), baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                supportedModels, 1, 0);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        return callImageEndpoint("/v1/stable-diffusion-xl/text-to-image", request, null);
    }

    @Override
    public ImageGenResult editImage(ImageEditRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", request.prompt());
        body.put("image", request.imageData());
        if (request.maskData() != null) body.put("mask_image", request.maskData());
        return callSyncImageRequest("/v1/stable-diffusion-xl/inpaint", body, request.model());
    }

    private ImageGenResult callImageEndpoint(String path, ImageGenRequest request, String extraParam) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", request.prompt());
        if (request.model() != null) body.put("model", request.model());
        if (request.width() != null) body.put("width", request.width());
        if (request.height() != null) body.put("height", request.height());
        return callSyncImageRequest(path, body, request.model());
    }

    private ImageGenResult callSyncImageRequest(String path, Map<String, Object> body, String model) {
        try {
            String json = objectMapper.writeValueAsString(body);
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofMillis(timeoutMs));
            authStrategy.apply(req);
            var resp = httpClient.send(req.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400)
                throw new TechnicalException(ErrorCode.AI_010, providerName() + " HTTP " + resp.statusCode());
            JsonNode root = objectMapper.readTree(resp.body());
            String url = root.path("url").asText(null);
            String b64 = root.path("image").asText(null);
            return new ImageGenResult(
                    List.of(new ImageGenResult.GeneratedImage(url, b64, null)),
                    model != null ? model : supportedModels.get(0), PROVIDER_NAME);
        } catch (TechnicalException e) { throw e; }
        catch (Exception e) { throw new TechnicalException(ErrorCode.AI_010, providerName() + " failed: " + e.getMessage()); }
    }

    @Override protected String submitJobEndpoint() { return baseUrl + "/v1/stable-diffusion-xl/text-to-image"; }
    @Override protected String parseSubmitResponse(JsonNode r) { return "sync"; }
    @Override protected String pollJobEndpoint(String id) { return baseUrl; }
    @Override protected AsyncJobStatus parsePollResponse(JsonNode r) {
        return new AsyncJobStatus(null, "completed", r.path("url").asText(null), null, 100, null);
    }
}
