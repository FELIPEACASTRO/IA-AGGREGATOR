package com.ia.aggregator.infrastructure.execution.persistence.repository;

import com.ia.aggregator.infrastructure.execution.persistence.entity.ToolJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ToolJpaRepository extends JpaRepository<ToolJpaEntity, UUID> {

    List<ToolJpaEntity> findByScope(String scope);
}
