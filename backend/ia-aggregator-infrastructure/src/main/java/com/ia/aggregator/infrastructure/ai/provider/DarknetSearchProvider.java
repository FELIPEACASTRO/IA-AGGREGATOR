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
 * DarkNet Search provider — darknet marketplace and forum search.
 *
 * <p>Supports: THREAT_INTEL_SEARCH
 * <p>Auth: Bearer token
 * <p>Compliance: Gated behind {@code app.ai.compliance.dark-web-enabled=true}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.darknetsearch.api-key")
public class DarknetSearchProvider extends AbstractSearchProvider
        implements ThreatIntelCapable {

    private static final String PROVIDER_NAME = "darknetsearch";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.THREAT_INTEL_SEARCH);

    public DarknetSearchProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.darknetsearch.api-key:}") String apiKey,
            @Value("${app.ai.providers.darknetsearch.base-url:https://api.darknetsearch.io}") String baseUrl,
            @Value("${app.ai.providers.darknetsearch.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.darknetsearch.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.darknetsearch.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("darknet-search"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ThreatIntelResult searchThreatIntel(ThreatIntelRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.query());
        if (request.maxResults() != null) body.put("limit", request.maxResults());
        if (request.dateFrom() != null) body.put("date_from", request.dateFrom());
        if (request.dateTo() != null) body.put("date_to", request.dateTo());
        if (request.sourceType() != null) body.put("category", request.sourceType());

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/v1/search", body));

        List<ThreatIntelHit> hits = new ArrayList<>();
        JsonNode data = response.path("data");
        if (data.isArray()) {
            for (JsonNode d : data) {
                hits.add(new ThreatIntelHit(
                        d.path("title").asText(null),
                        d.path("url").asText(null),
                        d.path("content").asText(null),
                        d.path("category").asText("darknet"),
                        d.path("risk_level").asText(null),
                        d.path("discovered_at").asText(null),
                        d.has("relevance_score") ? d.path("relevance_score").asDouble() : null
                ));
            }
        }

        int total = response.path("total").asInt(hits.size());
        return new ThreatIntelResult(hits, total, "darknet-search", PROVIDER_NAME);
    }
}
