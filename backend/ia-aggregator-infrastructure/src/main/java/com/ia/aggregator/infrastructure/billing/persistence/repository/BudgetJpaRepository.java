package com.ia.aggregator.infrastructure.billing.persistence.repository;

import com.ia.aggregator.infrastructure.billing.persistence.entity.BudgetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, UUID> {

    List<BudgetJpaEntity> findByOrgIdAndIsActiveTrue(UUID orgId);

    List<BudgetJpaEntity> findByOrgId(UUID orgId);
}
