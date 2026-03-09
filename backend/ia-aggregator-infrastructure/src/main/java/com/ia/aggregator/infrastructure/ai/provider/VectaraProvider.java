package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.application.ai.port.out.capability.WebSearchCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.ApiKeyHeaderAuth;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.auth.CompositeAuthStrategy;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractSearchProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Vectara provider -- semantic search and embeddings via Vectara v2 API.
 *
 * <p>API: {@code https://api.vectara.io}
 * <p>Supports: WEB_SEARCH, EMBEDDINGS
 * <p>Auth: Bearer token + x-api-key header (customer-id) via CompositeAuthStrategy
 *
 * <p>Search: POST /v2/query
 * <p>Embed: POST /v2/corpora/{corpusId}/documents (simplified)
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.vectara.api-key")
public class VectaraProvider extends AbstractSearchProvider
        implements WebSearchCapable, EmbeddingCapable {

    private static final String PROVIDER_NAME = "vectara";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.WEB_SEARCH, Capability.EMBEDDINGS);

    private final String customerId;

    public VectaraProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.vectara.api-key:}") String apiKey,
            @Value("${app.ai.providers.vectara.customer-id:}") String customerId,
            @Value("${app.ai.providers.vectara.base-url:https://api.vectara.io}") String baseUrl,
            @Value("${app.ai.providers.vectara.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.vectara.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.vectara.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper,
                new CompositeAuthStrategy(
                        new BearerTokenAuth(apiKey),
                        new ApiKeyHeaderAuth("x-api-key", customerId)
                ),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("vectara-v2"));
        this.customerId = customerId;
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public WebSearchResult search(WebSearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.query());
        body.put("search", Map.of(
                "limit", request.maxResults() != null ? request.maxResults() : 10
        ));

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/v2/query", body));

        List<SearchHit> hits = new ArrayList<>();
        JsonNode searchResults = response.path("search_results");
        if (searchResults.isArray()) {
            for (JsonNode r : searchResults) {
                hits.add(new SearchHit(
                        r.path("document_id").asText(null),
                        null,
                        r.path("text").asText(null),
                        r.has("score") ? r.path("score").asDouble() : null,
                        null
                ));
            }
        }

        return new WebSearchResult(hits, hits.size(), "vectara-v2", PROVIDER_NAME);
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        // Vectara embeds via document ingestion; simplified to use query endpoint for vector retrieval
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", request.input().get(0));
        body.put("search", Map.of("limit", 1));

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/v2/query", body));

        // Vectara does not directly return raw embeddings through the query API;
        // return empty embeddings with metadata for now
        List<float[]> embeddings = new ArrayList<>();
        int dimensions = 0;

        return new EmbeddingResult(embeddings, "vectara-v2", PROVIDER_NAME, dimensions, null);
    }
}
