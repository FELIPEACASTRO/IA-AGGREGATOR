package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.AvatarVideoRequest;
import com.ia.aggregator.application.ai.dto.AvatarVideoResult;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractAvatarVideoProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name = "app.ai.providers.tavus.api-key")
public class TavusProvider extends AbstractAvatarVideoProvider {

    public TavusProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.tavus.api-key:}") String apiKey,
            @Value("${app.ai.providers.tavus.base-url:https://api.tavus.io}") String baseUrl,
            @Value("${app.ai.providers.tavus.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.tavus.max-poll-attempts:60}") int maxPoll,
            @Value("${app.ai.providers.tavus.poll-interval-ms:5000}") long pollMs
    ) {
        super(om, cbr.circuitBreaker("aiProviderTavus"),
                new ApiKeyHeaderAuth("x-api-key", apiKey),
                baseUrl, timeoutMs, maxPoll, pollMs);
    }

    @Override public String providerName() { return "tavus"; }
    @Override protected String submitEndpoint() { return "/v2/videos"; }

    @Override
    protected String buildSubmitBody(AvatarVideoRequest req) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("script", req.script());
            if (req.avatarId() != null) body.put("replica_id", req.avatarId());
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override protected String parseJobId(JsonNode r) { return r.path("video_id").asText(); }
    @Override protected String pollEndpoint(String id) { return "/v2/videos/" + id; }
    @Override protected boolean isJobComplete(JsonNode r) { return "ready".equals(r.path("status").asText()); }
    @Override protected boolean isJobFailed(JsonNode r) { return "failed".equals(r.path("status").asText()); }

    @Override
    protected AvatarVideoResult parseResult(JsonNode r) {
        return new AvatarVideoResult(r.path("download_url").asText(null),
                r.path("duration").asDouble(0), providerName(), null);
    }
}
