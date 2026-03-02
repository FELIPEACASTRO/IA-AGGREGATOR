package com.ia.aggregator.infrastructure.auth.persistence.mapper;

import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import com.ia.aggregator.infrastructure.auth.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between domain User entity and JPA UserJpaEntity.
 * Keeps domain model free of JPA annotations.
 */
@Component
public class UserPersistenceMapper {

    public UserJpaEntity toJpaEntity(User domain) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setPersonalOrgId(domain.getOrgId());
        entity.setEmail(domain.getEmail());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setFullName(domain.getFullName());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setRole(domain.getRole().name());
        entity.setStatus(domain.getStatus().name());
        entity.setAuthProvider(domain.getAuthProvider().name());
        entity.setProviderUserId(domain.getProviderUserId());
        entity.setLocale(domain.getLocale());
        entity.setTimezone(domain.getTimezone());
        entity.setReferralCode(domain.getReferralCode());
        entity.setEmailVerified(domain.isEmailVerified());
        entity.setLastLoginAt(domain.getLastLoginAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public User toDomainEntity(UserJpaEntity jpa) {
        return User.reconstitute(
                jpa.getId(),
                jpa.getPersonalOrgId(),
                jpa.getEmail(),
                jpa.getPasswordHash(),
                jpa.getFullName(),
                jpa.getAvatarUrl(),
                UserRole.valueOf(jpa.getRole()),
                UserStatus.valueOf(jpa.getStatus()),
                AuthProvider.valueOf(jpa.getAuthProvider()),
                jpa.getProviderUserId(),
                jpa.getLocale(),
                jpa.getTimezone(),
                jpa.getReferralCode(),
                jpa.isEmailVerified(),
                jpa.getLastLoginAt(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }
}
