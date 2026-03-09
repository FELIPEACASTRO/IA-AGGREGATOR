package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.ThreatIntelCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractSearchProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Breachsense provider -- breach and credential leak intelligence.
 *
 * <p>API: {@code https://breachsense.com/api}
 * <p>Supports: THREAT_INTEL_SEARCH
 * <p>Auth: X-API-KEY header
 * <p>Compliance: Gated behind {@code security.compliance.dark-web-enabled=true}
 *
 * <p>GET /search?type={queryType}&query={query}
 */
@Component
@ConditionalOnProperty(name = "security.compliance.dark-web-enabled", havingValue = "true")
public class BreachsenseProvider extends AbstractSearchProvider
        implements ThreatIntelCapable {

    private static final String PROVIDER_NAME = "breachsense";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.THREAT_INTEL_SEARCH);

    public BreachsenseProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.breachsense.api-key:}") String apiKey,
            @Value("${app.ai.providers.breachsense.base-url:https://breachsense.com/api}") String baseUrl,
            @Value("${app.ai.providers.breachsense.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.breachsense.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.breachsense.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new ApiKeyHeaderAuth("X-API-KEY", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("breachsense-v1"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ThreatIntelResult searchThreatIntel(ThreatIntelRequest request) {
        String encodedQuery = URLEncoder.encode(request.query(), StandardCharsets.UTF_8);
        String queryType = request.sourceType() != null ? request.sourceType() : "auto";
        String endpoint = baseUrl + "/search?type=" + queryType + "&query=" + encodedQuery;

        JsonNode response = executeWithRetry(() -> sendGet(endpoint));

        List<ThreatIntelHit> hits = new ArrayList<>();
        JsonNode results = response.isArray() ? response : response.path("results");
        if (results.isArray()) {
            for (JsonNode r : results) {
                hits.add(new ThreatIntelHit(
                        r.path("source").asText(r.path("title").asText(null)),
                        null,
                        r.path("data").asText(r.path("snippet").asText(null)),
                        r.path("type").asText(null),
                        r.path("severity").asText(null),
                        r.path("date").asText(r.path("discovered_date").asText(null)),
                        r.has("score") ? r.path("score").asDouble() : null
                ));
            }
        }

        int total = response.has("total") ? response.path("total").asInt(hits.size()) : hits.size();
        return new ThreatIntelResult(hits, total, "breachsense-v1", PROVIDER_NAME);
    }
}
