package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.WebSearchCapable;
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
 * Brave Search provider -- web search via Brave Search API.
 *
 * <p>API: {@code https://api.search.brave.com}
 * <p>Supports: WEB_SEARCH
 * <p>Auth: X-Subscription-Token header
 *
 * <p>GET /res/v1/web/search?q={query}&count={maxResults}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.brave-search.api-key")
public class BraveSearchProvider extends AbstractSearchProvider
        implements WebSearchCapable {

    private static final String PROVIDER_NAME = "brave-search";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.WEB_SEARCH);

    public BraveSearchProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.brave-search.api-key:}") String apiKey,
            @Value("${app.ai.providers.brave-search.base-url:https://api.search.brave.com}") String baseUrl,
            @Value("${app.ai.providers.brave-search.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.brave-search.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.brave-search.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new ApiKeyHeaderAuth("X-Subscription-Token", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("brave-web-search"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public WebSearchResult search(WebSearchRequest request) {
        int count = request.maxResults() != null ? request.maxResults() : 10;
        String encodedQuery = URLEncoder.encode(request.query(), StandardCharsets.UTF_8);
        String endpoint = baseUrl + "/res/v1/web/search?q=" + encodedQuery + "&count=" + count;

        JsonNode response = executeWithRetry(() -> sendGet(endpoint));

        List<SearchHit> hits = new ArrayList<>();
        JsonNode results = response.path("web").path("results");
        if (results.isArray()) {
            for (JsonNode r : results) {
                hits.add(new SearchHit(
                        r.path("title").asText(null),
                        r.path("url").asText(null),
                        r.path("description").asText(null),
                        r.has("relevance_score") ? r.path("relevance_score").asDouble() : null,
                        r.path("page_age").asText(null)
                ));
            }
        }

        return new WebSearchResult(hits, hits.size(), "brave-web-search", PROVIDER_NAME);
    }
}
