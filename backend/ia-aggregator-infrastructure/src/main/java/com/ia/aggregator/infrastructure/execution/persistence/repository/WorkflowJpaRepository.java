package com.ia.aggregator.infrastructure.execution.persistence.repository;

import com.ia.aggregator.infrastructure.execution.persistence.entity.WorkflowJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowJpaRepository extends JpaRepository<WorkflowJpaEntity, UUID> {

    List<WorkflowJpaEntity> findByOrgId(UUID orgId);
}
