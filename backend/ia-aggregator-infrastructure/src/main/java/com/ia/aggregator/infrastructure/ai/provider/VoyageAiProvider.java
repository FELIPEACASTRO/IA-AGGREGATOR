package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.RerankCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.BearerTokenAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractOpenAiCompatibleProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Voyage AI provider — embeddings and rerank specialist.
 *
 * <p>Supports: EMBEDDINGS, RERANK
 * <p>API: {@code https://api.voyageai.com/v1}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.voyage.api-key")
public class VoyageAiProvider extends AbstractOpenAiCompatibleProvider implements RerankCapable {

    private static final String PROVIDER_NAME = "voyage";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.EMBEDDINGS, Capability.RERANK);

    public VoyageAiProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.voyage.api-key:}") String apiKey,
            @Value("${app.ai.providers.voyage.base-url:https://api.voyageai.com}") String baseUrl,
            @Value("${app.ai.providers.voyage.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.voyage.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.voyage.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.voyage.supported-models:voyage-3,voyage-3-lite,voyage-code-3}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderVoyage"),
                new BearerTokenAuth(apiKey),
                baseUrl, timeoutMs, retryAttempts, retryBackoffMs, supportedModels);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<Capability> capabilities() {
        return SUPPORTED_CAPABILITIES;
    }

    @Override
    protected String resolveDefaultEmbeddingModel() {
        return "voyage-3";
    }

    @Override
    public RerankResult rerank(RerankRequest request) {
        try {
            String model = request.model() != null ? request.model() : "rerank-2";
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("query", request.query());
            payload.put("documents", request.documents().stream().map(RerankDocument::text).toList());
            if (request.topN() != null) payload.put("top_k", request.topN());

            String responseBody = sendJsonRequest("/v1/rerank", payload);
            JsonNode root = objectMapper.readTree(responseBody);

            List<RerankResult.ScoredDocument> scored = new ArrayList<>();
            JsonNode results = root.path("data");
            if (results.isArray()) {
                for (JsonNode r : results) {
                    int index = r.path("index").asInt();
                    double score = r.path("relevance_score").asDouble();
                    RerankDocument doc = index < request.documents().size()
                            ? request.documents().get(index) : null;
                    scored.add(new RerankResult.ScoredDocument(index, score, doc));
                }
            }
            return new RerankResult(scored, model, providerName());
        } catch (TechnicalException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalException(ErrorCode.AI_009,
                    providerName() + " rerank failed: " + e.getMessage());
        }
    }
}
