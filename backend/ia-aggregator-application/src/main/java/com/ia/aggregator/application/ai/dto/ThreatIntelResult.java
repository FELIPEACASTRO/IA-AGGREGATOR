package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Result DTO for threat intelligence search.
 *
 * @param hits list of threat intelligence hits
 * @param totalResults total results available
 * @param modelUsed engine/model used
 * @param providerUsed provider that performed the search
 */
public record ThreatIntelResult(
        List<ThreatIntelHit> hits,
        Integer totalResults,
        String modelUsed,
        String providerUsed
) {
}
