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
 * Mixedbread provider — embeddings and rerank specialist.
 *
 * <p>Supports: EMBEDDINGS, RERANK
 * <p>API: {@code https://api.mixedbread.ai/v1}
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.mixedbread.api-key")
public class MixedbreadProvider extends AbstractOpenAiCompatibleProvider implements RerankCapable {

    private static final String PROVIDER_NAME = "mixedbread";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.EMBEDDINGS, Capability.RERANK);

    public MixedbreadProvider(
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${app.ai.providers.mixedbread.api-key:}") String apiKey,
            @Value("${app.ai.providers.mixedbread.base-url:https://api.mixedbread.ai}") String baseUrl,
            @Value("${app.ai.providers.mixedbread.timeout-ms:15000}") long timeoutMs,
            @Value("${app.ai.providers.mixedbread.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.mixedbread.retry-backoff-ms:250}") long retryBackoffMs,
            @Value("${app.ai.providers.mixedbread.supported-models:mxbai-embed-large-v1,mxbai-embed-2d-large-v1}") List<String> supportedModels
    ) {
        super(objectMapper,
                circuitBreakerRegistry.circuitBreaker("aiProviderMixedbread"),
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
        return "mxbai-embed-large-v1";
    }

    @Override
    public RerankResult rerank(RerankRequest request) {
        try {
            String model = request.model() != null ? request.model() : "mxbai-rerank-large-v1";
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("query", request.query());
            payload.put("input", request.documents().stream().map(RerankDocument::text).toList());
            if (request.topN() != null) payload.put("top_k", request.topN());

            String responseBody = sendJsonRequest("/v1/reranking", payload);
            JsonNode root = objectMapper.readTree(responseBody);

            List<RerankResult.ScoredDocument> scored = new ArrayList<>();
            JsonNode results = root.path("data");
            if (results.isArray()) {
                for (JsonNode r : results) {
                    int index = r.path("index").asInt();
                    double score = r.path("score").asDouble();
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
