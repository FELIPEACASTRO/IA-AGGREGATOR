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
 * FullHunt provider — attack surface and exposure intelligence.
 *
 * <p>API: {@code https://fullhunt.io/api/v1}
 * <p>Supports: THREAT_INTEL_SEARCH
 * <p>Auth: {@code X-API-KEY: {key}}
 * <p>Compliance: Gated behind {@code app.ai.compliance.dark-web-enabled=true}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.fullhunt.api-key")
public class FullHuntProvider extends AbstractSearchProvider
        implements ThreatIntelCapable {

    private static final String PROVIDER_NAME = "fullhunt";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.THREAT_INTEL_SEARCH);

    public FullHuntProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.fullhunt.api-key:}") String apiKey,
            @Value("${app.ai.providers.fullhunt.base-url:https://fullhunt.io/api/v1}") String baseUrl,
            @Value("${app.ai.providers.fullhunt.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.fullhunt.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.fullhunt.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new ApiKeyHeaderAuth("X-API-KEY", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("fullhunt-search"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ThreatIntelResult searchThreatIntel(ThreatIntelRequest request) {
        String query = URLEncoder.encode(request.query(), StandardCharsets.UTF_8);
        String endpoint = baseUrl + "/domain/" + query + "/subdomains";

        JsonNode response = executeWithRetry(() -> sendGet(endpoint));

        List<ThreatIntelHit> hits = new ArrayList<>();
        JsonNode hosts = response.path("hosts");
        if (hosts.isArray()) {
            for (JsonNode h : hosts) {
                hits.add(new ThreatIntelHit(
                        h.path("host").asText(null),
                        h.path("url").asText(null),
                        h.path("dns").path("a").path(0).asText(null),
                        "subdomain",
                        h.path("is_live").asBoolean() ? "low" : "medium",
                        null,
                        null
                ));
            }
        }

        int total = response.path("metadata").path("total_results").asInt(hits.size());
        return new ThreatIntelResult(hits, total, "fullhunt-search", PROVIDER_NAME);
    }
}
