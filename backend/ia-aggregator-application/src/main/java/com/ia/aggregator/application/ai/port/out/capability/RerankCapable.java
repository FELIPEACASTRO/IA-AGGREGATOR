package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.RerankRequest;
import com.ia.aggregator.application.ai.dto.RerankResult;

/**
 * Capability interface for document relevance re-ranking.
 * Providers implementing this can score and reorder documents by relevance to a query.
 */
public interface RerankCapable {

    /**
     * Re-ranks a set of documents based on relevance to the query.
     *
     * @param request the rerank request containing query, documents, and parameters
     * @return the rerank result with scored and ordered documents
     */
    RerankResult rerank(RerankRequest request);
}
