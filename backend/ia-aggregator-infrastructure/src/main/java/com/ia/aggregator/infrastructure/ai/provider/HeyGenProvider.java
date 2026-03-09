package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.AvatarVideoRequest;
import com.ia.aggregator.application.ai.dto.AvatarVideoResult;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractAvatarVideoProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnProperty(name = "app.ai.providers.heygen.api-key")
public class HeyGenProvider extends AbstractAvatarVideoProvider {

    public HeyGenProvider(
            ObjectMapper om, CircuitBreakerRegistry cbr,
            @Value("${app.ai.providers.heygen.api-key:}") String apiKey,
            @Value("${app.ai.providers.heygen.base-url:https://api.heygen.com}") String baseUrl,
            @Value("${app.ai.providers.heygen.timeout-ms:120000}") long timeoutMs,
            @Value("${app.ai.providers.heygen.max-poll-attempts:60}") int maxPoll,
            @Value("${app.ai.providers.heygen.poll-interval-ms:5000}") long pollMs
    ) {
        super(om, cbr.circuitBreaker("aiProviderHeygen"), new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, maxPoll, pollMs);
    }

    @Override public String providerName() { return "heygen"; }

    @Override protected String submitEndpoint() { return "/v2/video/generate"; }

    @Override
    protected String buildSubmitBody(AvatarVideoRequest req) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            Map<String, Object> videoInput = new LinkedHashMap<>();
            Map<String, Object> character = new LinkedHashMap<>();
            character.put("type", "avatar");
            character.put("avatar_id", req.avatarId() != null ? req.avatarId() : "default");
            Map<String, Object> voice = new LinkedHashMap<>();
            voice.put("type", "text");
            voice.put("input_text", req.script());
            if (req.voiceId() != null) voice.put("voice_id", req.voiceId());
            videoInput.put("character", character);
            videoInput.put("voice", voice);
            body.put("video_inputs", List.of(videoInput));
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override protected String parseJobId(JsonNode r) { return r.path("data").path("video_id").asText(); }
    @Override protected String pollEndpoint(String id) { return "/v1/video_status.get?video_id=" + id; }
    @Override protected boolean isJobComplete(JsonNode r) { return "completed".equals(r.path("data").path("status").asText()); }
    @Override protected boolean isJobFailed(JsonNode r) { return "failed".equals(r.path("data").path("status").asText()); }

    @Override
    protected AvatarVideoResult parseResult(JsonNode r) {
        return new AvatarVideoResult(
                r.path("data").path("video_url").asText(null),
                r.path("data").path("duration").asDouble(0),
                providerName(), null);
    }
}
