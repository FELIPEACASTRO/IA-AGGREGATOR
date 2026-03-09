package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.WebSearchCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractSearchProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Exa (formerly Metaphor) provider — neural web search.
 *
 * <p>API: {@code https://api.exa.ai}
 * <p>Supports: WEB_SEARCH
 * <p>Auth: Bearer token
 *
 * <p>POST /search → {@code {"query": "...", "numResults": N}}
 * <p>Response: {@code {"results": [{"title": "...", "url": "...", "text": "..."}]}}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.exa.api-key")
public class ExaSearchProvider extends AbstractSearchProvider
        implements WebSearchCapable {

    private static final String PROVIDER_NAME = "exa";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.WEB_SEARCH);

    public ExaSearchProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.exa.api-key:}") String apiKey,
            @Value("${app.ai.providers.exa.base-url:https://api.exa.ai}") String baseUrl,
            @Value("${app.ai.providers.exa.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.exa.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.exa.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("exa-search"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public WebSearchResult search(WebSearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.query());
        body.put("numResults", request.maxResults() != null ? request.maxResults() : 10);
        body.put("contents", Map.of("text", true));
        if (request.domains() != null) body.put("includeDomains", List.of(request.domains().split(",")));

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/search", body));

        List<SearchHit> hits = new ArrayList<>();
        JsonNode results = response.path("results");
        if (results.isArray()) {
            for (JsonNode r : results) {
                hits.add(new SearchHit(
                        r.path("title").asText(null),
                        r.path("url").asText(null),
                        r.path("text").asText(null),
                        r.has("score") ? r.path("score").asDouble() : null,
                        r.path("publishedDate").asText(null)
                ));
            }
        }

        return new WebSearchResult(hits, hits.size(), "exa-search", PROVIDER_NAME);
    }
}
