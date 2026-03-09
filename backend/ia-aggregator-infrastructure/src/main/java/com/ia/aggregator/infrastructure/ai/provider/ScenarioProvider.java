package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ImageGenerationCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractMediaProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name = "app.ai.providers.scenario.api-key")
public class ScenarioProvider extends AbstractMediaProvider implements ImageGenerationCapable {

    private static final String PROVIDER_NAME = "scenario";
    private static final Set<Capability> SUPPORTED = EnumSet.of(Capability.IMAGE_GENERATION);
    private final String modelId;

    public ScenarioProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.scenario.api-key:}") String apiKey,
            @Value("${app.ai.providers.scenario.base-url:https://api.cloud.scenario.com}") String baseUrl,
            @Value("${app.ai.providers.scenario.model-id:}") String modelId,
            @Value("${app.ai.providers.scenario.timeout-ms:60000}") long timeoutMs,
            @Value("${app.ai.providers.scenario.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.scenario.retry-backoff-ms:1000}") long retryBackoffMs,
            @Value("${app.ai.providers.scenario.supported-models:scenario-custom}") List<String> models,
            @Value("${app.ai.providers.scenario.max-poll-attempts:40}") int maxPollAttempts,
            @Value("${app.ai.providers.scenario.poll-interval-ms:3000}") long pollIntervalMs
    ) {
        super(om, cbr.circuitBreaker("aiProviderScenario"), new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, models, maxPollAttempts, pollIntervalMs);
        this.modelId = modelId;
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED; }

    @Override
    public ImageGenResult generateImage(ImageGenRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("prompt", request.prompt());
        if (request.numberOfImages() != null) body.put("numSamples", request.numberOfImages());
        AsyncJobStatus status = executeWithRetry(() -> submitAndPoll(body));
        return new ImageGenResult(
                List.of(new ImageGenResult.GeneratedImage(status.resultUrl(), null, null)),
                "scenario-custom", PROVIDER_NAME);
    }

    @Override protected String submitJobEndpoint() {
        return baseUrl + "/v1/models/" + modelId + "/inferences";
    }
    @Override protected String parseSubmitResponse(JsonNode r) {
        return r.path("inference").path("id").asText();
    }
    @Override protected String pollJobEndpoint(String id) {
        return baseUrl + "/v1/models/" + modelId + "/inferences/" + id;
    }
    @Override protected AsyncJobStatus parsePollResponse(JsonNode r) {
        String status = r.path("inference").path("status").asText("pending");
        if ("succeeded".equals(status)) {
            String url = r.path("inference").path("images").path(0).path("url").asText(null);
            return new AsyncJobStatus(null, "completed", url, null, 100, null);
        }
        if ("failed".equals(status))
            return new AsyncJobStatus(null, "failed", null, "Job failed", null, null);
        return new AsyncJobStatus(null, "processing", null, null, null, null);
    }
}
