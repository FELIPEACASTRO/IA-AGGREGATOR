package com.ia.aggregator.infrastructure.auth.persistence.mapper;

import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import com.ia.aggregator.infrastructure.auth.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserPersistenceMapperTest {

    private final UserPersistenceMapper mapper = new UserPersistenceMapper();

    @Test
    void toJpaEntity_shouldMapAllFields() {
        User user = createTestUser();

        UserJpaEntity jpa = mapper.toJpaEntity(user);

        assertEquals(user.getId(), jpa.getId());
        assertEquals(user.getOrgId(), jpa.getPersonalOrgId());
        assertEquals(user.getEmail(), jpa.getEmail());
        assertEquals(user.getPasswordHash(), jpa.getPasswordHash());
        assertEquals(user.getFullName(), jpa.getFullName());
        assertEquals(user.getAvatarUrl(), jpa.getAvatarUrl());
        assertEquals(user.getRole(), jpa.getRole());
        assertEquals(user.getStatus(), jpa.getStatus());
        assertEquals(user.getAuthProvider(), jpa.getAuthProvider());
        assertEquals(user.getProviderUserId(), jpa.getProviderUserId());
        assertEquals(user.getLocale(), jpa.getLocale());
        assertEquals(user.getTimezone(), jpa.getTimezone());
        assertEquals(user.getReferralCode(), jpa.getReferralCode());
        assertEquals(user.isEmailVerified(), jpa.isEmailVerified());
        assertEquals(user.getLastLoginAt(), jpa.getLastLoginAt());
        assertEquals(user.getFailedLoginCount(), jpa.getFailedLoginCount());
        assertEquals(user.getLockedUntil(), jpa.getLockedUntil());
    }

    @Test
    void toDomainEntity_shouldMapAllFields() {
        UserJpaEntity jpa = createTestJpaEntity();

        User user = mapper.toDomainEntity(jpa);

        assertEquals(jpa.getId(), user.getId());
        assertEquals(jpa.getPersonalOrgId(), user.getOrgId());
        assertEquals(jpa.getEmail(), user.getEmail());
        assertEquals(jpa.getPasswordHash(), user.getPasswordHash());
        assertEquals(jpa.getFullName(), user.getFullName());
        assertEquals(jpa.getAvatarUrl(), user.getAvatarUrl());
        assertEquals(jpa.getRole(), user.getRole());
        assertEquals(jpa.getStatus(), user.getStatus());
        assertEquals(jpa.getAuthProvider(), user.getAuthProvider());
        assertEquals(jpa.getProviderUserId(), user.getProviderUserId());
        assertEquals(jpa.getLocale(), user.getLocale());
        assertEquals(jpa.getTimezone(), user.getTimezone());
        assertEquals(jpa.getReferralCode(), user.getReferralCode());
        assertEquals(jpa.isEmailVerified(), user.isEmailVerified());
        assertEquals(jpa.getLastLoginAt(), user.getLastLoginAt());
        assertEquals(jpa.getFailedLoginCount(), user.getFailedLoginCount());
        assertEquals(jpa.getLockedUntil(), user.getLockedUntil());
    }

    @Test
    void roundTrip_shouldPreserveData() {
        User original = createTestUser();

        UserJpaEntity jpa = mapper.toJpaEntity(original);
        User reconstructed = mapper.toDomainEntity(jpa);

        assertEquals(original.getId(), reconstructed.getId());
        assertEquals(original.getEmail(), reconstructed.getEmail());
        assertEquals(original.getRole(), reconstructed.getRole());
        assertEquals(original.getStatus(), reconstructed.getStatus());
        assertEquals(original.getAuthProvider(), reconstructed.getAuthProvider());
    }

    @Test
    void toJpaEntity_shouldStoreEnumsDirectly() {
        User user = createTestUser();

        UserJpaEntity jpa = mapper.toJpaEntity(user);

        // Enums should be stored as enum objects (not strings)
        assertSame(UserRole.ADMIN, jpa.getRole());
        assertSame(UserStatus.ACTIVE, jpa.getStatus());
        assertSame(AuthProvider.LOCAL, jpa.getAuthProvider());
    }

    private User createTestUser() {
        UUID id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Instant now = Instant.now();
        return User.reconstitute(
                id, orgId, "test@test.com", "hashPwd",
                "Test User", "https://avatar.url",
                UserRole.ADMIN, UserStatus.ACTIVE, AuthProvider.LOCAL,
                null, "pt-BR", "UTC",
                "REF12345", true, now, 0, null, now, now
        );
    }

    private UserJpaEntity createTestJpaEntity() {
        UserJpaEntity jpa = new UserJpaEntity();
        jpa.setId(UUID.randomUUID());
        jpa.setPersonalOrgId(UUID.randomUUID());
        jpa.setEmail("test@test.com");
        jpa.setPasswordHash("hashPwd");
        jpa.setFullName("Test User");
        jpa.setAvatarUrl("https://avatar.url");
        jpa.setRole(UserRole.ADMIN);
        jpa.setStatus(UserStatus.ACTIVE);
        jpa.setAuthProvider(AuthProvider.LOCAL);
        jpa.setLocale("pt-BR");
        jpa.setTimezone("UTC");
        jpa.setReferralCode("REF12345");
        jpa.setEmailVerified(true);
        jpa.setLastLoginAt(Instant.now());
        jpa.setFailedLoginCount(2);
        jpa.setLockedUntil(null);
        jpa.setCreatedAt(Instant.now());
        jpa.setUpdatedAt(Instant.now());
        return jpa;
    }
}
