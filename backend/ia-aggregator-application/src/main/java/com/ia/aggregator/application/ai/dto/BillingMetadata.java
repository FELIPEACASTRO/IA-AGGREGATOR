package com.ia.aggregator.application.ai.dto;

import com.ia.aggregator.domain.ai.Capability;

import java.time.Instant;

/**
 * FinOps billing record emitted after each AI provider call.
 *
 * <p>Used by the telemetry layer to record cost metrics in Micrometer
 * ({@code ai.provider.cost.estimate.usd} counter).
 * NOTE: Prices are never hardcoded here — {@code estimatedCostUsd} is computed
 * externally from configuration, not from class-level constants.
 *
 * @param provider         provider name (e.g., {@code "openai"})
 * @param model            model used (e.g., {@code "gpt-4o"})
 * @param capability       capability invoked
 * @param estimatedCostUsd advisory cost estimate; null if price is unknown
 * @param timestamp        wall-clock time of the provider call completion
 */
public record BillingMetadata(
        String provider,
        String model,
        Capability capability,
        Double estimatedCostUsd,
        Instant timestamp
) {
}
