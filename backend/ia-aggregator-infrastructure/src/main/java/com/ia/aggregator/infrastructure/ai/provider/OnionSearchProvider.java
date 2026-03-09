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
 * Onion Search provider — .onion (Tor) dark web search engine.
 *
 * <p>Supports: THREAT_INTEL_SEARCH
 * <p>Auth: {@code X-API-KEY: {key}}
 * <p>Compliance: Gated behind {@code app.ai.compliance.dark-web-enabled=true}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.onionsearch.api-key")
public class OnionSearchProvider extends AbstractSearchProvider
        implements ThreatIntelCapable {

    private static final String PROVIDER_NAME = "onionsearch";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.THREAT_INTEL_SEARCH);

    public OnionSearchProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.onionsearch.api-key:}") String apiKey,
            @Value("${app.ai.providers.onionsearch.base-url:https://api.onionsearchengine.com}") String baseUrl,
            @Value("${app.ai.providers.onionsearch.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.onionsearch.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.onionsearch.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new ApiKeyHeaderAuth("X-API-KEY", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("onion-search"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public ThreatIntelResult searchThreatIntel(ThreatIntelRequest request) {
        String query = URLEncoder.encode(request.query(), StandardCharsets.UTF_8);
        String endpoint = baseUrl + "/search?q=" + query;
        if (request.maxResults() != null) endpoint += "&limit=" + request.maxResults();

        String finalEndpoint = endpoint;
        JsonNode response = executeWithRetry(() -> sendGet(finalEndpoint));

        List<ThreatIntelHit> hits = new ArrayList<>();
        JsonNode results = response.path("results");
        if (results.isArray()) {
            for (JsonNode r : results) {
                hits.add(new ThreatIntelHit(
                        r.path("title").asText(null),
                        r.path("link").asText(null),
                        r.path("snippet").asText(null),
                        "onion",
                        null,
                        r.path("date").asText(null),
                        null
                ));
            }
        }

        return new ThreatIntelResult(hits, hits.size(), "onion-search", PROVIDER_NAME);
    }
}
