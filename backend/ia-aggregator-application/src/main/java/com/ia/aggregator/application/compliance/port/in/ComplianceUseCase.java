package com.ia.aggregator.application.compliance.port.in;

import com.ia.aggregator.domain.compliance.ConsentRecord;
import com.ia.aggregator.domain.compliance.DataErasureRequest;

import java.util.List;
import java.util.UUID;

/**
 * Use case for LGPD/GDPR compliance management.
 */
public interface ComplianceUseCase {

    // Consent management
    ConsentRecord grantConsent(UUID userId, UUID orgId, ConsentRecord.ConsentType type,
                                String purpose, String ipAddress);

    ConsentRecord revokeConsent(UUID userId, ConsentRecord.ConsentType type);

    List<ConsentRecord> getConsents(UUID userId);

    boolean hasConsent(UUID userId, ConsentRecord.ConsentType type);

    // Right to erasure
    DataErasureRequest requestErasure(UUID userId, UUID orgId, String reason);

    DataErasureRequest getErasureRequest(UUID requestId);

    List<DataErasureRequest> listErasureRequests(UUID orgId, int page, int size);

    DataErasureRequest processErasure(UUID requestId, UUID processedBy);

    // Data portability
    byte[] exportUserData(UUID userId);
}
