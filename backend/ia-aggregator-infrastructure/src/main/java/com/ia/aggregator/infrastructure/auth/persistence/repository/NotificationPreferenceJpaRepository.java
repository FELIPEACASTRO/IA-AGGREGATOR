package com.ia.aggregator.infrastructure.auth.persistence.repository;

import com.ia.aggregator.infrastructure.auth.persistence.entity.NotificationPreferenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceJpaRepository extends JpaRepository<NotificationPreferenceJpaEntity, UUID> {

    Optional<NotificationPreferenceJpaEntity> findByUserId(UUID userId);
}
