package com.ia.aggregator.domain.compliance;

import java.time.Instant;
import java.util.UUID;

/**
 * LGPD/GDPR consent record.
 */
public record ConsentRecord(
        UUID id,
        UUID userId,
        UUID orgId,
        ConsentType type,
        boolean granted,
        String purpose,
        String ipAddress,
        Instant grantedAt,
        Instant revokedAt
) {
    public enum ConsentType {
        DATA_PROCESSING, MARKETING, ANALYTICS, AI_TRAINING, DATA_SHARING
    }
}
