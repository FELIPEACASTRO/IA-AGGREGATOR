package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.AvatarVideoRequest;
import com.ia.aggregator.application.ai.dto.AvatarVideoResult;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractAvatarVideoProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name = "app.ai.providers.d-id.api-key")
public class DIdProvider extends AbstractAvatarVideoProvider {

    public DIdProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.d-id.api-key:}") String apiKey,
            @Value("${app.ai.providers.d-id.base-url:https://api.d-id.com}") String baseUrl,
            @Value("${app.ai.providers.d-id.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.d-id.max-poll-attempts:60}") int maxPoll,
            @Value("${app.ai.providers.d-id.poll-interval-ms:5000}") long pollMs
    ) {
        super(om, cbr.circuitBreaker("aiProviderDId"), new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, maxPoll, pollMs);
    }

    @Override public String providerName() { return "d-id"; }
    @Override protected String submitEndpoint() { return "/talks"; }

    @Override
    protected String buildSubmitBody(AvatarVideoRequest req) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            Map<String, Object> script = new LinkedHashMap<>();
            script.put("type", "text");
            script.put("input", req.script());
            if (req.voiceId() != null) {
                script.put("provider", Map.of("type", "microsoft", "voice_id", req.voiceId()));
            }
            body.put("script", script);
            if (req.avatarId() != null) body.put("source_url", req.avatarId());
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override protected String parseJobId(JsonNode r) { return r.path("id").asText(); }
    @Override protected String pollEndpoint(String id) { return "/talks/" + id; }
    @Override protected boolean isJobComplete(JsonNode r) { return "done".equals(r.path("status").asText()); }
    @Override protected boolean isJobFailed(JsonNode r) { return "error".equals(r.path("status").asText()); }

    @Override
    protected AvatarVideoResult parseResult(JsonNode r) {
        return new AvatarVideoResult(r.path("result_url").asText(null),
                r.path("duration").asDouble(0), providerName(), null);
    }
}
