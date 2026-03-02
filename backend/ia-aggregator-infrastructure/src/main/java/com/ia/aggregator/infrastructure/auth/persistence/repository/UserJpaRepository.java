package com.ia.aggregator.infrastructure.auth.persistence.repository;

import com.ia.aggregator.infrastructure.auth.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<UserJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<UserJpaEntity> findByReferralCodeAndDeletedAtIsNull(String referralCode);
}
