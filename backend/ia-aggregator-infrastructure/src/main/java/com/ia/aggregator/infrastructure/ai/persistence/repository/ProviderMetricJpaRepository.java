package com.ia.aggregator.infrastructure.ai.persistence.repository;

import com.ia.aggregator.infrastructure.ai.persistence.entity.ProviderMetricJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderMetricJpaRepository extends JpaRepository<ProviderMetricJpaEntity, UUID> {

    List<ProviderMetricJpaEntity> findByProviderIdAndPeriod(UUID providerId, String period);

    Optional<ProviderMetricJpaEntity> findByProviderIdAndModelIdAndPeriod(UUID providerId, String modelId, String period);
}
