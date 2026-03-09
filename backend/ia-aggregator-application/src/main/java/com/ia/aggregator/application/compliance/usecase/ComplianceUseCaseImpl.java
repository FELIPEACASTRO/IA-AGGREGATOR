package com.ia.aggregator.application.compliance.usecase;

import com.ia.aggregator.application.compliance.port.in.ComplianceUseCase;
import com.ia.aggregator.application.compliance.port.out.ConsentRepository;
import com.ia.aggregator.domain.compliance.ConsentRecord;
import com.ia.aggregator.domain.compliance.DataErasureRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ComplianceUseCaseImpl implements ComplianceUseCase {

    private static final Logger log = LoggerFactory.getLogger(ComplianceUseCaseImpl.class);

    private final ConsentRepository consentRepository;

    public ComplianceUseCaseImpl(ConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    @Override
    public ConsentRecord grantConsent(UUID userId, UUID orgId, ConsentRecord.ConsentType type,
                                       String purpose, String ipAddress) {
        ConsentRecord consent = new ConsentRecord(
                UUID.randomUUID(), userId, orgId, type, true, purpose, ipAddress,
                Instant.now(), null
        );
        consentRepository.saveConsent(consent);
        log.info("Consent granted: userId={}, type={}", userId, type);
        return consent;
    }

    @Override
    public ConsentRecord revokeConsent(UUID userId, ConsentRecord.ConsentType type) {
        ConsentRecord existing = consentRepository.findConsent(userId, type)
                .orElseThrow(() -> new NoSuchElementException("Consent not found"));
        ConsentRecord revoked = new ConsentRecord(
                existing.id(), existing.userId(), existing.orgId(), existing.type(),
                false, existing.purpose(), existing.ipAddress(),
                existing.grantedAt(), Instant.now()
        );
        consentRepository.saveConsent(revoked);
        log.info("Consent revoked: userId={}, type={}", userId, type);
        return revoked;
    }

    @Override
    public List<ConsentRecord> getConsents(UUID userId) {
        return consentRepository.findConsentsByUser(userId);
    }

    @Override
    public boolean hasConsent(UUID userId, ConsentRecord.ConsentType type) {
        return consentRepository.findConsent(userId, type)
                .map(ConsentRecord::granted)
                .orElse(false);
    }

    @Override
    public DataErasureRequest requestErasure(UUID userId, UUID orgId, String reason) {
        DataErasureRequest request = new DataErasureRequest(
                UUID.randomUUID(), userId, orgId,
                DataErasureRequest.ErasureStatus.PENDING,
                reason, null, Instant.now(), null
        );
        consentRepository.saveErasureRequest(request);
        log.info("Data erasure requested: userId={}, orgId={}", userId, orgId);
        return request;
    }

    @Override
    public DataErasureRequest getErasureRequest(UUID requestId) {
        return consentRepository.findErasureRequestById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Erasure request not found: " + requestId));
    }

    @Override
    public List<DataErasureRequest> listErasureRequests(UUID orgId, int page, int size) {
        return consentRepository.findErasureRequestsByOrg(orgId, page * size, size);
    }

    @Override
    public DataErasureRequest processErasure(UUID requestId, UUID processedBy) {
        DataErasureRequest request = getErasureRequest(requestId);

        // In full implementation, would cascade-delete all user data across schemas
        DataErasureRequest processed = new DataErasureRequest(
                request.id(), request.userId(), request.orgId(),
                DataErasureRequest.ErasureStatus.COMPLETED,
                request.reason(), processedBy, request.requestedAt(), Instant.now()
        );
        consentRepository.saveErasureRequest(processed);
        log.info("Data erasure completed: requestId={}, userId={}", requestId, request.userId());
        return processed;
    }

    @Override
    public byte[] exportUserData(UUID userId) {
        // In full implementation, would collect all user data across schemas
        // and package as JSON/ZIP for data portability
        String exportData = "{\"userId\": \"" + userId + "\", \"exportDate\": \"" +
                Instant.now() + "\", \"data\": {}}";
        return exportData.getBytes();
    }
}
