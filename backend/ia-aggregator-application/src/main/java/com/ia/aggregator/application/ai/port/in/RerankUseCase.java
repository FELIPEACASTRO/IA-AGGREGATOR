package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.RerankRequest;
import com.ia.aggregator.application.ai.dto.RerankResult;

/**
 * Input port for document reranking.
 * Reorders documents by relevance to a query.
 */
public interface RerankUseCase {
    RerankResult execute(RerankRequest request);
}
