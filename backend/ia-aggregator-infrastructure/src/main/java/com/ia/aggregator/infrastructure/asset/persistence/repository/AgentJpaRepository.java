package com.ia.aggregator.infrastructure.asset.persistence.repository;

import com.ia.aggregator.infrastructure.asset.persistence.entity.AgentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentJpaRepository extends JpaRepository<AgentJpaEntity, UUID> {

    List<AgentJpaEntity> findByOrgId(UUID orgId);

    List<AgentJpaEntity> findByOrgIdAndCategory(UUID orgId, String category);

    List<AgentJpaEntity> findByIsPrebuiltTrueAndIsActiveTrue();
}
