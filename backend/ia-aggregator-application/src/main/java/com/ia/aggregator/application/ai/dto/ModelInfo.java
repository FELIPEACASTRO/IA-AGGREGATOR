package com.ia.aggregator.application.ai.dto;

import com.ia.aggregator.domain.ai.Capability;

import java.util.Set;

/**
 * Information about a specific AI model available in the platform.
 *
 * @param modelId model identifier (e.g., "gpt-4o-mini")
 * @param providerName provider that offers this model
 * @param capabilities capabilities available for this model
 * @param contextWindow maximum context window in tokens (null if unknown)
 * @param pricingTier pricing tier (e.g., "free", "standard", "premium")
 */
public record ModelInfo(
        String modelId,
        String providerName,
        Set<Capability> capabilities,
        Integer contextWindow,
        String pricingTier
) {
}
