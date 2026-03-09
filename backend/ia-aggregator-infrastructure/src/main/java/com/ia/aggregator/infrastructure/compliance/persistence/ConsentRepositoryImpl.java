package com.ia.aggregator.infrastructure.compliance.persistence;

import com.ia.aggregator.application.compliance.port.out.ConsentRepository;
import com.ia.aggregator.domain.compliance.ConsentRecord;
import com.ia.aggregator.domain.compliance.DataErasureRequest;
import com.ia.aggregator.infrastructure.compliance.persistence.entity.ConsentRecordJpaEntity;
import com.ia.aggregator.infrastructure.compliance.persistence.entity.ErasureRequestJpaEntity;
import com.ia.aggregator.infrastructure.compliance.persistence.repository.ConsentRecordJpaRepository;
import com.ia.aggregator.infrastructure.compliance.persistence.repository.ErasureRequestJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConsentRepositoryImpl implements ConsentRepository {

    private final ConsentRecordJpaRepository consentJpa;
    private final ErasureRequestJpaRepository erasureJpa;

    public ConsentRepositoryImpl(ConsentRecordJpaRepository consentJpa,
                                 ErasureRequestJpaRepository erasureJpa) {
        this.consentJpa = consentJpa;
        this.erasureJpa = erasureJpa;
    }

    @Override
    public void saveConsent(ConsentRecord consent) {
        ConsentRecordJpaEntity entity = consentJpa.findById(consent.id())
                .orElseGet(ConsentRecordJpaEntity::new);
        entity.setId(consent.id());
        entity.setUserId(consent.userId());
        entity.setConsentType(consent.type() != null ? consent.type().name() : null);
        entity.setGranted(consent.granted());
        entity.setDocumentUrl(consent.purpose());
        entity.setIpAddress(consent.ipAddress());
        entity.setGrantedAt(consent.grantedAt());
        entity.setRevokedAt(consent.revokedAt());
        consentJpa.save(entity);
    }

    @Override
    public List<ConsentRecord> findConsentsByUser(UUID userId) {
        return consentJpa.findByUserId(userId).stream()
                .map(this::toConsentDomain)
                .toList();
    }

    @Override
    public Optional<ConsentRecord> findConsent(UUID userId, ConsentRecord.ConsentType type) {
        return consentJpa.findFirstByUserIdAndConsentTypeOrderByCreatedAtDesc(userId, type.name())
                .map(this::toConsentDomain);
    }

    @Override
    public void saveErasureRequest(DataErasureRequest request) {
        ErasureRequestJpaEntity entity = erasureJpa.findById(request.id())
                .orElseGet(ErasureRequestJpaEntity::new);
        entity.setId(request.id());
        entity.setUserId(request.userId());
        entity.setStatus(request.status() != null ? request.status().name() : "PENDING");
        entity.setNotes(request.reason());
        entity.setProcessedBy(request.processedBy());
        entity.setRequestedAt(request.requestedAt());
        entity.setCompletedAt(request.completedAt());
        erasureJpa.save(entity);
    }

    @Override
    public Optional<DataErasureRequest> findErasureRequestById(UUID id) {
        return erasureJpa.findById(id).map(this::toErasureDomain);
    }

    @Override
    public List<DataErasureRequest> findErasureRequestsByOrg(UUID orgId, int offset, int limit) {
        int safeLimit = Math.max(1, limit);
        int page = offset / safeLimit;
        return erasureJpa.findByOrgId(orgId, PageRequest.of(page, safeLimit)).stream()
                .map(this::toErasureDomain)
                .toList();
    }

    private ConsentRecord toConsentDomain(ConsentRecordJpaEntity entity) {
        ConsentRecord.ConsentType type;
        try {
            type = ConsentRecord.ConsentType.valueOf(entity.getConsentType());
        } catch (Exception e) {
            type = ConsentRecord.ConsentType.DATA_PROCESSING;
        }

        return new ConsentRecord(
                entity.getId(),
                entity.getUserId(),
                null, // orgId not stored directly in consent_records table
                type,
                entity.isGranted(),
                entity.getDocumentUrl(),
                entity.getIpAddress(),
                entity.getGrantedAt(),
                entity.getRevokedAt()
        );
    }

    private DataErasureRequest toErasureDomain(ErasureRequestJpaEntity entity) {
        DataErasureRequest.ErasureStatus status;
        try {
            status = DataErasureRequest.ErasureStatus.valueOf(entity.getStatus());
        } catch (Exception e) {
            status = DataErasureRequest.ErasureStatus.PENDING;
        }

        return new DataErasureRequest(
                entity.getId(),
                entity.getUserId(),
                null, // orgId resolved through user relationship
                status,
                entity.getNotes(),
                entity.getProcessedBy(),
                entity.getRequestedAt(),
                entity.getCompletedAt()
        );
    }
}
