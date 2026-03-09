package com.ia.aggregator.domain.platform;

import java.time.Instant;
import java.util.UUID;

/**
 * A batch API job for processing multiple requests at a discount.
 */
public record BatchJob(
        UUID id,
        UUID orgId,
        String webhookUrl,
        int totalRequests,
        int completedRequests,
        int failedRequests,
        BatchStatus status,
        double totalCostUsd,
        double discountPercent,
        Instant createdAt,
        Instant completedAt
) {
    public enum BatchStatus {
        QUEUED, PROCESSING, COMPLETED, PARTIALLY_FAILED, FAILED, CANCELED
    }
}
