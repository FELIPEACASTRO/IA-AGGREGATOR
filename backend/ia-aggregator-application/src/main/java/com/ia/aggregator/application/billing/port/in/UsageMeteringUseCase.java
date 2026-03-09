package com.ia.aggregator.application.billing.port.in;

import com.ia.aggregator.domain.billing.UsageRecord;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for recording and querying AI usage for billing.
 */
public interface UsageMeteringUseCase {

    void recordUsage(UsageRecord record);

    List<UsageRecord> getUsage(UUID orgId, Instant from, Instant to);

    /**
     * Get aggregated usage summary by provider and model.
     */
    Map<String, UsageSummary> getUsageSummary(UUID orgId, Instant from, Instant to);

    double getTotalCost(UUID orgId, Instant from, Instant to);

    record UsageSummary(String key, long requests, long inputTokens,
                         long outputTokens, double totalCostUsd) {}
}
