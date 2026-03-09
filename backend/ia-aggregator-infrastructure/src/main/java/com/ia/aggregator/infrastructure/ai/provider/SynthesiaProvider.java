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
@ConditionalOnProperty(name = "app.ai.providers.synthesia.api-key")
public class SynthesiaProvider extends AbstractAvatarVideoProvider {

    public SynthesiaProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.synthesia.api-key:}") String apiKey,
            @Value("${app.ai.providers.synthesia.base-url:https://api.synthesia.io}") String baseUrl,
            @Value("${app.ai.providers.synthesia.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.synthesia.max-poll-attempts:60}") int maxPoll,
            @Value("${app.ai.providers.synthesia.poll-interval-ms:10000}") long pollMs
    ) {
        super(om, cbr.circuitBreaker("aiProviderSynthesia"), new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, maxPoll, pollMs);
    }

    @Override public String providerName() { return "synthesia"; }
    @Override protected String submitEndpoint() { return "/v2/videos"; }

    @Override
    protected String buildSubmitBody(AvatarVideoRequest req) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("title", "Generated Video");
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("scriptText", req.script());
            if (req.avatarId() != null) input.put("avatar", req.avatarId());
            if (req.language() != null) input.put("language", req.language());
            body.put("input", List.of(input));
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override protected String parseJobId(JsonNode r) { return r.path("id").asText(); }
    @Override protected String pollEndpoint(String id) { return "/v2/videos/" + id; }
    @Override protected boolean isJobComplete(JsonNode r) { return "complete".equals(r.path("status").asText()); }
    @Override protected boolean isJobFailed(JsonNode r) { return "failed".equals(r.path("status").asText()); }

    @Override
    protected AvatarVideoResult parseResult(JsonNode r) {
        return new AvatarVideoResult(r.path("download").asText(null),
                r.path("duration").asDouble(0), providerName(), null);
    }
}
