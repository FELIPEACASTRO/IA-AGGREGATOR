package com.ia.aggregator.domain.compliance;

import java.time.Instant;
import java.util.UUID;

/**
 * GDPR/LGPD right-to-erasure request.
 */
public record DataErasureRequest(
        UUID id,
        UUID userId,
        UUID orgId,
        ErasureStatus status,
        String reason,
        UUID processedBy,
        Instant requestedAt,
        Instant completedAt
) {
    public enum ErasureStatus {
        PENDING, PROCESSING, COMPLETED, REJECTED
    }
}
