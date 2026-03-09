package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.EmbeddingCapable;
import com.ia.aggregator.application.ai.port.out.capability.RerankCapable;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractSearchProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Jina AI provider — embeddings and document re-ranking.
 *
 * <p>API: {@code https://api.jina.ai}
 * <p>Supports: EMBEDDINGS, RERANK
 * <p>Auth: Bearer token
 *
 * <p>Embeddings: POST /v1/embeddings (OpenAI-compatible format)
 * <p>Rerank: POST /v1/rerank (Cohere-compatible format)
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.jina.api-key")
public class JinaAiProvider extends AbstractSearchProvider
        implements EmbeddingCapable, RerankCapable {

    private static final String PROVIDER_NAME = "jina";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.EMBEDDINGS, Capability.RERANK);

    public JinaAiProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.jina.api-key:}") String apiKey,
            @Value("${app.ai.providers.jina.base-url:https://api.jina.ai}") String baseUrl,
            @Value("${app.ai.providers.jina.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.jina.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.jina.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper, new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("jina-embeddings-v3", "jina-reranker-v2-base-multilingual"));
    }

    @Override public String providerName() { return PROVIDER_NAME; }
    @Override public Set<Capability> capabilities() { return SUPPORTED_CAPABILITIES; }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        String model = request.model() != null ? request.model() : "jina-embeddings-v3";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", request.input());
        if (request.dimensions() != null) body.put("dimensions", request.dimensions());

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/v1/embeddings", body));

        List<float[]> embeddings = new ArrayList<>();
        int dimensions = 0;
        JsonNode data = response.path("data");
        if (data.isArray()) {
            for (JsonNode item : data) {
                JsonNode embArr = item.path("embedding");
                float[] vector = new float[embArr.size()];
                for (int i = 0; i < embArr.size(); i++) {
                    vector[i] = (float) embArr.get(i).asDouble();
                }
                embeddings.add(vector);
                if (dimensions == 0) dimensions = vector.length;
            }
        }

        Integer totalTokens = response.path("usage").path("total_tokens").asInt(0);

        return new EmbeddingResult(embeddings, model, PROVIDER_NAME, dimensions,
                totalTokens > 0 ? totalTokens : null);
    }

    @Override
    public RerankResult rerank(RerankRequest request) {
        String model = request.model() != null ? request.model() : "jina-reranker-v2-base-multilingual";

        List<Map<String, String>> docs = request.documents().stream()
                .map(d -> Map.of("text", d.text()))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("query", request.query());
        body.put("documents", docs);
        if (request.topN() != null) body.put("top_n", request.topN());

        JsonNode response = executeWithRetry(() -> sendJsonPost(baseUrl + "/v1/rerank", body));

        List<RerankResult.ScoredDocument> results = new ArrayList<>();
        JsonNode resultsNode = response.path("results");
        if (resultsNode.isArray()) {
            for (JsonNode r : resultsNode) {
                int index = r.path("index").asInt();
                double score = r.path("relevance_score").asDouble();
                RerankDocument doc = index < request.documents().size()
                        ? request.documents().get(index) : null;
                results.add(new RerankResult.ScoredDocument(index, score, doc));
            }
        }

        return new RerankResult(results, model, PROVIDER_NAME);
    }
}
