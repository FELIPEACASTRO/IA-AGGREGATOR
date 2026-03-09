package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ThreatIntelCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractSearchProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Twingly provider — dark web intelligence and monitoring.
 *
 * <p>API: {@code https://api.twingly.com}
 * <p>Supports: THREAT_INTEL_SEARCH
 * <p>Auth: Bearer token
 * <p>Compliance: Gated behind {@code app.ai.compliance.dark-web-enabled=true}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.twingly.api-key")
public class TwinglyProvider extends AbstractSearchProvider
        implements ThreatIntelCapable {

    private static final String PROVIDER_NAME = "twingly";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.THREAT_INTEL_SEARCH);

    public TwinglyProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.twingly.api-key:}") String apiKey,
            @Value("${app.ai.providers.twingly.base-url:https://api.twingly.com}") String baseUrl,
            @Value("${app.ai.providers.twingly.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.twingly.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.twingly.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("twingly-search"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ThreatIntelResult searchThreatIntel(ThreatIntelRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.query());
        if (request.maxResults() != null) body.put("page_size", request.maxResults());
        if (request.dateFrom() != null) body.put("start_date", request.dateFrom());
        if (request.dateTo() != null) body.put("end_date", request.dateTo());

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/v3/search", body));

        List<ThreatIntelHit> hits = new ArrayList<>();
        JsonNode posts = response.path("posts");
        if (posts.isArray()) {
            for (JsonNode p : posts) {
                hits.add(new ThreatIntelHit(
                        p.path("title").asText(null),
                        p.path("url").asText(null),
                        p.path("text").asText(null),
                        p.path("source_type").asText("forum"),
                        null,
                        p.path("indexed_at").asText(null),
                        null
                ));
            }
        }

        int total = response.path("total_results").asInt(hits.size());
        return new ThreatIntelResult(hits, total, "twingly-search", PROVIDER_NAME);
    }
}
