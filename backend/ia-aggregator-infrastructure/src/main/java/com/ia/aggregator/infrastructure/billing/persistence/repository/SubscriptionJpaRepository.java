package com.ia.aggregator.infrastructure.billing.persistence.repository;

import com.ia.aggregator.infrastructure.billing.persistence.entity.SubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    Optional<SubscriptionJpaEntity> findByOrgIdAndStatus(UUID orgId, String status);

    List<SubscriptionJpaEntity> findByUserId(UUID userId);
}
