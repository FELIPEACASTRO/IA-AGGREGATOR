package com.ia.aggregator.infrastructure.auth.persistence.mapper;

import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.infrastructure.auth.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between domain User entity and JPA UserJpaEntity.
 * Keeps domain model free of JPA annotations.
 */
@Component
public class UserPersistenceMapper {

    /**
     * Creates a NEW JPA entity from a domain entity.
     * Use only for INSERT (new user). For UPDATE, use updateJpaEntity().
     */
    public UserJpaEntity toJpaEntity(User domain) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        updateJpaEntity(entity, domain);
        return entity;
    }

    /**
     * Updates an EXISTING JPA entity with domain fields.
     * Preserves JPA-only fields (displayName, phone, metadata, etc.).
     */
    public void updateJpaEntity(UserJpaEntity entity, User domain) {
        entity.setPersonalOrgId(domain.getOrgId());
        entity.setEmail(domain.getEmail());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setFullName(domain.getFullName());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setRole(domain.getRole());
        entity.setStatus(domain.getStatus());
        entity.setAuthProvider(domain.getAuthProvider());
        entity.setProviderUserId(domain.getProviderUserId());
        entity.setLocale(domain.getLocale());
        entity.setTimezone(domain.getTimezone());
        entity.setReferralCode(domain.getReferralCode());
        entity.setEmailVerified(domain.isEmailVerified());
        entity.setLastLoginAt(domain.getLastLoginAt());
        entity.setFailedLoginCount(domain.getFailedLoginCount());
        entity.setLockedUntil(domain.getLockedUntil());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
    }

    public User toDomainEntity(UserJpaEntity jpa) {
        return User.reconstitute(
                jpa.getId(),
                jpa.getPersonalOrgId(),
                jpa.getEmail(),
                jpa.getPasswordHash(),
                jpa.getFullName(),
                jpa.getAvatarUrl(),
                jpa.getRole(),
                jpa.getStatus(),
                jpa.getAuthProvider(),
                jpa.getProviderUserId(),
                jpa.getLocale(),
                jpa.getTimezone(),
                jpa.getReferralCode(),
                jpa.isEmailVerified(),
                jpa.getLastLoginAt(),
                jpa.getFailedLoginCount(),
                jpa.getLockedUntil(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }
}
