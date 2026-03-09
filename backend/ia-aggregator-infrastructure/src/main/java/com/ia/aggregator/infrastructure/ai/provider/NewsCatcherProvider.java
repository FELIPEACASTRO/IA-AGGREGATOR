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
 * NewsCatcher provider — news search API.
 *
 * <p>API: {@code https://v3-api.newscatcherapi.com}
 * <p>Supports: WEB_SEARCH
 * <p>Auth: {@code x-api-key: {key}}
 *
 * <p>POST /api/search → news articles matching query
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.newscatcher.api-key")
public class NewsCatcherProvider extends AbstractSearchProvider
        implements WebSearchCapable {

    private static final String PROVIDER_NAME = "newscatcher";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.WEB_SEARCH);

    public NewsCatcherProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.newscatcher.api-key:}") String apiKey,
            @Value("${app.ai.providers.newscatcher.base-url:https://v3-api.newscatcherapi.com}") String baseUrl,
            @Value("${app.ai.providers.newscatcher.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.newscatcher.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.newscatcher.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new ApiKeyHeaderAuth("x-api-key", apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("newscatcher-v3"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public WebSearchResult search(WebSearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("q", request.query());
        body.put("page_size", request.maxResults() != null ? request.maxResults() : 10);
        if (request.dateRange() != null) body.put("from_", request.dateRange());
        if (request.domains() != null) body.put("sources", request.domains());

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/api/search", body));

        List<SearchHit> hits = new ArrayList<>();
        JsonNode articles = response.path("articles");
        if (articles.isArray()) {
            for (JsonNode a : articles) {
                hits.add(new SearchHit(
                        a.path("title").asText(null),
                        a.path("link").asText(null),
                        a.path("excerpt").asText(null),
                        a.has("score") ? a.path("score").asDouble() : null,
                        a.path("published_date").asText(null)
                ));
            }
        }

        int total = response.path("total_hits").asInt(hits.size());
        return new WebSearchResult(hits, total, "newscatcher-v3", PROVIDER_NAME);
    }
}
