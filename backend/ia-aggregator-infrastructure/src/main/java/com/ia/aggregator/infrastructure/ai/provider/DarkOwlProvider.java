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
 * DarkOwl provider — dark web threat intelligence.
 *
 * <p>API: {@code https://api.darkowl.com}
 * <p>Supports: THREAT_INTEL_SEARCH
 * <p>Auth: Bearer token
 * <p>Compliance: Gated behind {@code app.ai.compliance.dark-web-enabled=true}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.darkowl.api-key")
public class DarkOwlProvider extends AbstractSearchProvider
        implements ThreatIntelCapable {

    private static final String PROVIDER_NAME = "darkowl";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.THREAT_INTEL_SEARCH);

    public DarkOwlProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.darkowl.api-key:}") String apiKey,
            @Value("${app.ai.providers.darkowl.base-url:https://api.darkowl.com}") String baseUrl,
            @Value("${app.ai.providers.darkowl.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.darkowl.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.darkowl.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("darkowl-search"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ThreatIntelResult searchThreatIntel(ThreatIntelRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.query());
        if (request.maxResults() != null) body.put("limit", request.maxResults());
        if (request.dateFrom() != null) body.put("from", request.dateFrom());
        if (request.dateTo() != null) body.put("to", request.dateTo());
        if (request.sourceType() != null) body.put("source_type", request.sourceType());

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/api/v1/search", body));

        List<ThreatIntelHit> hits = new ArrayList<>();
        JsonNode results = response.path("results");
        if (results.isArray()) {
            for (JsonNode r : results) {
                hits.add(new ThreatIntelHit(
                        r.path("title").asText(null),
                        r.path("url").asText(null),
                        r.path("body").asText(null),
                        r.path("source_type").asText(null),
                        r.path("severity").asText(null),
                        r.path("discovered_at").asText(null),
                        r.has("score") ? r.path("score").asDouble() : null
                ));
            }
        }

        int total = response.path("total").asInt(hits.size());
        return new ThreatIntelResult(hits, total, "darkowl-search", PROVIDER_NAME);
    }
}
