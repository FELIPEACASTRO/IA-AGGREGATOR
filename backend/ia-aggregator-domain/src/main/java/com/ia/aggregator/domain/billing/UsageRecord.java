package com.ia.aggregator.domain.billing;

import java.time.Instant;
import java.util.UUID;

/**
 * A usage record for metering and billing.
 *
 * @param id             Record ID
 * @param orgId          Organization
 * @param userId         User who made the request
 * @param capability     AI capability used
 * @param provider       Provider name
 * @param model          Model name
 * @param inputTokens    Input tokens consumed
 * @param outputTokens   Output tokens consumed
 * @param costUsd        Computed cost in USD
 * @param timestamp      When the usage occurred
 */
public record UsageRecord(
        UUID id,
        UUID orgId,
        UUID userId,
        String capability,
        String provider,
        String model,
        int inputTokens,
        int outputTokens,
        double costUsd,
        Instant timestamp
) {
    public UsageRecord {
        if (id == null) id = UUID.randomUUID();
        if (timestamp == null) timestamp = Instant.now();
    }
}
