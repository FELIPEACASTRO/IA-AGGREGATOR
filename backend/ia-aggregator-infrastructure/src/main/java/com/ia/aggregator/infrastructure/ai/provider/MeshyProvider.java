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
@ConditionalOnProperty(name = "app.ai.providers.meshy.api-key")
public class MeshyProvider extends AbstractThreeDProvider {

    public MeshyProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.meshy.api-key:}") String apiKey,
            @Value("${app.ai.providers.meshy.base-url:https://api.meshy.ai}") String baseUrl,
            @Value("${app.ai.providers.meshy.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.meshy.max-poll-attempts:60}") int maxPoll,
            @Value("${app.ai.providers.meshy.poll-interval-ms:5000}") long pollMs
    ) {
        super(om, cbr.circuitBreaker("aiProviderMeshy"), new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, maxPoll, pollMs);
    }

    @Override public String providerName() { return "meshy"; }
    @Override protected String submitEndpoint() { return "/v2/text-to-3d"; }

    @Override
    protected String buildSubmitBody(ThreeDGenerationRequest req) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("prompt", req.prompt());
            body.put("mode", "preview");
            if (req.outputFormat() != null) body.put("output_format", req.outputFormat());
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override protected String parseJobId(JsonNode r) { return r.path("result").asText(); }
    @Override protected String pollEndpoint(String id) { return "/v2/text-to-3d/" + id; }
    @Override protected boolean isJobComplete(JsonNode r) { return "SUCCEEDED".equals(r.path("status").asText()); }
    @Override protected boolean isJobFailed(JsonNode r) { return "FAILED".equals(r.path("status").asText()); }

    @Override
    protected ThreeDGenerationResult parseResult(JsonNode r) {
        return new ThreeDGenerationResult(
                r.path("model_urls").path("glb").asText(null),
                r.path("thumbnail_url").asText(null),
                "glb", providerName(), null);
    }
}
