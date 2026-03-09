package com.ia.aggregator.infrastructure.platform.persistence.repository;

import com.ia.aggregator.infrastructure.platform.persistence.entity.BatchJobJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchJobJpaRepository extends JpaRepository<BatchJobJpaEntity, UUID> {

    List<BatchJobJpaEntity> findByOrgId(UUID orgId, Pageable pageable);
}
