package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.ThreeDGenerationRequest;
import com.ia.aggregator.application.ai.dto.ThreeDGenerationResult;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractThreeDProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name = "app.ai.providers.tripo.api-key")
public class TripoProvider extends AbstractThreeDProvider {

    public TripoProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.tripo.api-key:}") String apiKey,
            @Value("${app.ai.providers.tripo.base-url:https://api.tripo3d.ai}") String baseUrl,
            @Value("${app.ai.providers.tripo.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.tripo.max-poll-attempts:60}") int maxPoll,
            @Value("${app.ai.providers.tripo.poll-interval-ms:5000}") long pollMs
    ) {
        super(om, cbr.circuitBreaker("aiProviderTripo"), new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, maxPoll, pollMs);
    }

    @Override public String providerName() { return "tripo"; }
    @Override protected String submitEndpoint() { return "/v2/openapi/task"; }

    @Override
    protected String buildSubmitBody(ThreeDGenerationRequest req) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("type", req.imageUrl() != null ? "image_to_model" : "text_to_model");
            Map<String, Object> draft = new LinkedHashMap<>();
            if (req.prompt() != null) draft.put("prompt", req.prompt());
            if (req.imageUrl() != null) draft.put("file", Map.of("url", req.imageUrl()));
            body.put("draft_model_task", draft);
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override protected String parseJobId(JsonNode r) { return r.path("data").path("task_id").asText(); }
    @Override protected String pollEndpoint(String id) { return "/v2/openapi/task/" + id; }
    @Override protected boolean isJobComplete(JsonNode r) { return "success".equals(r.path("data").path("status").asText()); }
    @Override protected boolean isJobFailed(JsonNode r) { return "failed".equals(r.path("data").path("status").asText()); }

    @Override
    protected ThreeDGenerationResult parseResult(JsonNode r) {
        JsonNode output = r.path("data").path("output");
        return new ThreeDGenerationResult(
                output.path("model").asText(null),
                output.path("rendered_image").asText(null),
                "glb", providerName(), null);
    }
}
