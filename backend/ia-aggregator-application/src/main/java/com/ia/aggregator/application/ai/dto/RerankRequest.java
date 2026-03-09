package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for document re-ranking.
 *
 * @param query the query to rank documents against (required)
 * @param documents list of documents to re-rank (required, non-empty)
 * @param model preferred rerank model (optional)
 * @param topN return only top N results (optional — all if null)
 */
public record RerankRequest(
        @NotBlank(message = "query is required")
        String query,
        @NotEmpty(message = "documents are required")
        List<RerankDocument> documents,
        String model,
        Integer topN
) {
}
