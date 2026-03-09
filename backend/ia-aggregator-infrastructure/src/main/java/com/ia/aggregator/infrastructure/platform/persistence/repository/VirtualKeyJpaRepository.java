package com.ia.aggregator.infrastructure.platform.persistence.repository;

import com.ia.aggregator.infrastructure.platform.persistence.entity.VirtualKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VirtualKeyJpaRepository extends JpaRepository<VirtualKeyJpaEntity, UUID> {

    Optional<VirtualKeyJpaEntity> findByKeyHash(String keyHash);

    List<VirtualKeyJpaEntity> findByOrgId(UUID orgId);
}
