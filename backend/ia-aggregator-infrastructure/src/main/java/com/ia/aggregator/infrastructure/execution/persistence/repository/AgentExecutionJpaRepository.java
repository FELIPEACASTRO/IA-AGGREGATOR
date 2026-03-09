package com.ia.aggregator.infrastructure.execution.persistence.repository;

import com.ia.aggregator.infrastructure.execution.persistence.entity.AgentExecutionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentExecutionJpaRepository extends JpaRepository<AgentExecutionJpaEntity, UUID> {

    List<AgentExecutionJpaEntity> findByOrgId(UUID orgId, Pageable pageable);
}
