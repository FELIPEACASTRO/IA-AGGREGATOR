package com.ia.aggregator.infrastructure.billing.persistence.repository;

import com.ia.aggregator.infrastructure.billing.persistence.entity.BillingPlanJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingPlanJpaRepository extends JpaRepository<BillingPlanJpaEntity, UUID> {

    List<BillingPlanJpaEntity> findByIsActiveTrueOrderBySortOrder();

    Optional<BillingPlanJpaEntity> findBySlug(String slug);

    Optional<BillingPlanJpaEntity> findByTier(String tier);
}
