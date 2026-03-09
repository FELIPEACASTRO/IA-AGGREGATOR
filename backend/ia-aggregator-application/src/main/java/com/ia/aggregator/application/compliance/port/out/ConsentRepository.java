package com.ia.aggregator.application.compliance.port.out;

import com.ia.aggregator.domain.compliance.ConsentRecord;
import com.ia.aggregator.domain.compliance.DataErasureRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository {
    void saveConsent(ConsentRecord consent);
    List<ConsentRecord> findConsentsByUser(UUID userId);
    Optional<ConsentRecord> findConsent(UUID userId, ConsentRecord.ConsentType type);
    void saveErasureRequest(DataErasureRequest request);
    Optional<DataErasureRequest> findErasureRequestById(UUID id);
    List<DataErasureRequest> findErasureRequestsByOrg(UUID orgId, int offset, int limit);
}
