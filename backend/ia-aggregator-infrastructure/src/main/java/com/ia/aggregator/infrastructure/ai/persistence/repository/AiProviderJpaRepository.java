package com.ia.aggregator.infrastructure.ai.persistence.repository;

import com.ia.aggregator.infrastructure.ai.persistence.entity.AiProviderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiProviderJpaRepository extends JpaRepository<AiProviderJpaEntity, UUID> {

    Optional<AiProviderJpaEntity> findByName(String name);

    List<AiProviderJpaEntity> findByStatus(String status);
}
