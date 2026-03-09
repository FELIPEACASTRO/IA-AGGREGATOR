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
@ConditionalOnProperty(name = "app.ai.providers.runware.api-key")
public class RunwareProvider extends AbstractMediaProvider implements ImageGenerationCapable {

    private static final String PROVIDER_NAME = "runware";
    private static final Set<Capability> SUPPORTED = EnumSet.of(Capability.IMAGE_GENERATION);

    public RunwareProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.runware.api-key:}") String apiKey,
            @Value("${app.ai.providers.runware.base-url:https://api.runware.ai}") String baseUrl,
            @Value("${app.ai.providers.runware.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.runware.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.runware.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.runware.supported-models:runware:100@1,civitai:133005@288982}") List<String> models
    ) {
        super(om, cbr.circuitBreaker("aiProviderRunware"), new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, models, 1, 0);
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED; }

    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        try {
            String model = request.model() != null ? request.model() : supportedModels.get(0);
            List<Map<String, Object>> tasks = new ArrayList<>();
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("taskType", "imageInference");
            task.put("taskUUID", UUID.randomUUID().toString());
            task.put("positivePrompt", request.prompt());
            task.put("model", model);
            if (request.width() != null) task.put("width", request.width());
            if (request.height() != null) task.put("height", request.height());
            if (request.numberOfImages() != null) task.put("numberResults", request.numberOfImages());
            tasks.add(task);

            String json = objectMapper.writeValueAsString(tasks);
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofMillis(timeoutMs));
            authStrategy.apply(req);
            var resp = httpClient.send(req.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400)
                throw new TechnicalException(ErrorCode.AI_010, providerName() + " HTTP " + resp.statusCode());
            JsonNode root = objectMapper.readTree(resp.body());
            List<ImageGenResult.GeneratedImage> images = new ArrayList<>();
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode img : data) {
                    images.add(new ImageGenResult.GeneratedImage(img.path("imageURL").asText(null), null, null));
                }
            }
            return new ImageGenResult(images, model, PROVIDER_NAME);
        } catch (TechnicalException e) { throw e; }
        catch (Exception e) { throw new TechnicalException(ErrorCode.AI_010, providerName() + " failed: " + e.getMessage()); }
    }

    @Override protected String submitJobEndpoint() { return baseUrl + "/v1"; }
    @Override protected String parseSubmitResponse(JsonNode r) { return "sync"; }
    @Override protected String pollJobEndpoint(String id) { return baseUrl; }
    @Override protected AsyncJobStatus parsePollResponse(JsonNode r) {
        return new AsyncJobStatus(null, "completed", null, null, 100, null);
    }
}
