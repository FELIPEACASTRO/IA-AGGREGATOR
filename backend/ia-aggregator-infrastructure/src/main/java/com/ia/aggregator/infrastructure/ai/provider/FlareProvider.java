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
 * Flare provider — threat intelligence and dark web monitoring.
 *
 * <p>API: {@code https://api.flare.io}
 * <p>Supports: THREAT_INTEL_SEARCH
 * <p>Auth: Bearer token
 * <p>Compliance: Gated behind {@code app.ai.compliance.dark-web-enabled=true}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.flare.api-key")
public class FlareProvider extends AbstractSearchProvider
        implements ThreatIntelCapable {

    private static final String PROVIDER_NAME = "flare";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.THREAT_INTEL_SEARCH);

    public FlareProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.flare.api-key:}") String apiKey,
            @Value("${app.ai.providers.flare.base-url:https://api.flare.io}") String baseUrl,
            @Value("${app.ai.providers.flare.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.flare.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.flare.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("flare-search"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ThreatIntelResult searchThreatIntel(ThreatIntelRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.query());
        if (request.maxResults() != null) body.put("size", request.maxResults());
        if (request.dateFrom() != null) body.put("from_date", request.dateFrom());
        if (request.dateTo() != null) body.put("to_date", request.dateTo());
        if (request.sourceType() != null) body.put("source_type", request.sourceType());

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/v2/search", body));

        List<ThreatIntelHit> hits = new ArrayList<>();
        JsonNode items = response.path("items");
        if (items.isArray()) {
            for (JsonNode item : items) {
                hits.add(new ThreatIntelHit(
                        item.path("title").asText(null),
                        item.path("source_url").asText(null),
                        item.path("content").asText(null),
                        item.path("source_type").asText(null),
                        item.path("severity").asText(null),
                        item.path("created_at").asText(null),
                        item.has("relevance") ? item.path("relevance").asDouble() : null
                ));
            }
        }

        int total = response.path("total").asInt(hits.size());
        return new ThreatIntelResult(hits, total, "flare-search", PROVIDER_NAME);
    }
}
