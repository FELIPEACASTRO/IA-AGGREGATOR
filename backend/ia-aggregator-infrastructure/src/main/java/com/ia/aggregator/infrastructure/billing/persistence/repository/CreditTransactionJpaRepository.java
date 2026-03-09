package com.ia.aggregator.infrastructure.billing.persistence.repository;

import com.ia.aggregator.infrastructure.billing.persistence.entity.CreditTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreditTransactionJpaRepository extends JpaRepository<CreditTransactionJpaEntity, UUID> {

    List<CreditTransactionJpaEntity> findByOrgIdOrderByCreatedAtDesc(UUID orgId);
}
