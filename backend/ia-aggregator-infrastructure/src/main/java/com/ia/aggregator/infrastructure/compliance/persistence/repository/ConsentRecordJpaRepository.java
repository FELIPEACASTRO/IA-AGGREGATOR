package com.ia.aggregator.infrastructure.compliance.persistence.repository;

import com.ia.aggregator.infrastructure.compliance.persistence.entity.ConsentRecordJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRecordJpaRepository extends JpaRepository<ConsentRecordJpaEntity, UUID> {

    List<ConsentRecordJpaEntity> findByUserId(UUID userId);

    Optional<ConsentRecordJpaEntity> findFirstByUserIdAndConsentTypeOrderByCreatedAtDesc(UUID userId, String consentType);
}
