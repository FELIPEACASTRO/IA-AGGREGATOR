package com.ia.aggregator.infrastructure.ai.persistence.repository;

import com.ia.aggregator.infrastructure.ai.persistence.entity.AiModelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiModelJpaRepository extends JpaRepository<AiModelJpaEntity, UUID> {

    List<AiModelJpaEntity> findByProviderIdAndIsActiveTrue(UUID providerId);

    List<AiModelJpaEntity> findByIsActiveTrueOrderByProviderIdAscModelIdAsc();

    List<AiModelJpaEntity> findByIsDefaultTrue();

    Optional<AiModelJpaEntity> findByModelId(String modelId);

    Optional<AiModelJpaEntity> findByProviderIdAndModelId(UUID providerId, String modelId);
}
