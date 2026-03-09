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

import java.util.*;

/**
 * Tavily provider — AI-optimized search for agents and RAG pipelines.
 *
 * <p>API: {@code https://api.tavily.com}
 * <p>Supports: WEB_SEARCH
 * <p>Auth: API key in request body (also added as header for consistency)
 *
 * <p>POST /search → {@code {"api_key": "...", "query": "...", "max_results": N}}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.tavily.api-key")
public class TavilyProvider extends AbstractSearchProvider
        implements WebSearchCapable {

    private static final String PROVIDER_NAME = "tavily";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.WEB_SEARCH);

    private final String apiKey;

    public TavilyProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.tavily.api-key:}") String apiKey,
            @Value("${app.ai.providers.tavily.base-url:https://api.tavily.com}") String baseUrl,
            @Value("${app.ai.providers.tavily.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.tavily.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.tavily.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new ApiKeyHeaderAuth("Authorization", "Bearer " + apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("tavily-search"));
        this.apiKey = apiKey;
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public WebSearchResult search(WebSearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api_key", apiKey);
        body.put("query", request.query());
        body.put("max_results", request.maxResults() != null ? request.maxResults() : 10);
        body.put("include_answer", false);
        body.put("include_raw_content", false);
        if (request.domains() != null) {
            body.put("include_domains", List.of(request.domains().split(",")));
        }

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/search", body));

        List<SearchHit> hits = new ArrayList<>();
        JsonNode results = response.path("results");
        if (results.isArray()) {
            for (JsonNode r : results) {
                hits.add(new SearchHit(
                        r.path("title").asText(null),
                        r.path("url").asText(null),
                        r.path("content").asText(null),
                        r.has("score") ? r.path("score").asDouble() : null,
                        r.path("published_date").asText(null)
                ));
            }
        }

        return new WebSearchResult(hits, hits.size(), "tavily-search", PROVIDER_NAME);
    }
}
