package com.ia.aggregator.domain.auth.entity;

import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void register_shouldCreateUserWithCorrectDefaults() {
        User user = User.register("Test@Email.com", "hashedPwd", "John Doe");

        assertNotNull(user.getId());
        assertEquals("test@email.com", user.getEmail());
        assertEquals("hashedPwd", user.getPasswordHash());
        assertEquals("John Doe", user.getFullName());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
        assertEquals(AuthProvider.LOCAL, user.getAuthProvider());
        assertEquals("pt-BR", user.getLocale());
        assertEquals("America/Sao_Paulo", user.getTimezone());
        assertFalse(user.isEmailVerified());
        assertNotNull(user.getReferralCode());
        assertEquals(8, user.getReferralCode().length());
        assertNull(user.getLastLoginAt());
        assertNull(user.getOrgId());
        assertNull(user.getAvatarUrl());
    }

    @Test
    void register_shouldEmitUserRegisteredEvent() {
        User user = User.register("user@test.com", "hash", "Test User");

        assertEquals(1, user.getDomainEvents().size());
    }

    @Test
    void register_shouldTrimAndLowercaseEmail() {
        User user = User.register("  USER@TEST.COM  ", "hash", "Name");

        assertEquals("user@test.com", user.getEmail());
    }

    @Test
    void registerOAuth_shouldCreateActiveVerifiedUser() {
        User user = User.registerOAuth("oauth@test.com", "OAuth User",
                AuthProvider.GOOGLE, "google-123", "https://avatar.url");

        assertEquals("oauth@test.com", user.getEmail());
        assertEquals("OAuth User", user.getFullName());
        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
        assertEquals("google-123", user.getProviderUserId());
        assertEquals("https://avatar.url", user.getAvatarUrl());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isEmailVerified());
        assertEquals(1, user.getDomainEvents().size());
    }

    @Test
    void verifyEmail_shouldSetEmailVerifiedAndActivateIfPending() {
        User user = User.register("user@test.com", "hash", "Name");
        assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
        assertFalse(user.isEmailVerified());

        user.verifyEmail();

        assertTrue(user.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void verifyEmail_shouldNotChangeStatusIfNotPending() {
        User user = createActiveUser();
        user.suspend();
        assertEquals(UserStatus.SUSPENDED, user.getStatus());

        user.verifyEmail();

        assertTrue(user.isEmailVerified());
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
    }

    @Test
    void recordLogin_shouldUpdateLastLoginAt() {
        User user = createActiveUser();
        assertNull(user.getLastLoginAt());

        user.recordLogin();

        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void changePassword_shouldUpdatePasswordHash() {
        User user = createActiveUser();

        user.changePassword("newHash");

        assertEquals("newHash", user.getPasswordHash());
    }

    @Test
    void changePassword_shouldThrowForOAuthUser() {
        User user = User.registerOAuth("oauth@test.com", "Name",
                AuthProvider.GOOGLE, "id", null);

        assertThrows(IllegalStateException.class, () -> user.changePassword("newHash"));
    }

    @Test
    void updateProfile_shouldUpdateNonNullFields() {
        User user = createActiveUser();

        user.updateProfile("New Name", "https://new-avatar.url", "en-US", "America/New_York");

        assertEquals("New Name", user.getFullName());
        assertEquals("https://new-avatar.url", user.getAvatarUrl());
        assertEquals("en-US", user.getLocale());
        assertEquals("America/New_York", user.getTimezone());
    }

    @Test
    void updateProfile_shouldIgnoreNullAndBlankFields() {
        User user = createActiveUser();
        String originalName = user.getFullName();
        String originalLocale = user.getLocale();

        user.updateProfile(null, null, "", "  ");

        assertEquals(originalName, user.getFullName());
        assertEquals(originalLocale, user.getLocale());
    }

    @Test
    void deactivate_shouldSetStatusToInactive() {
        User user = createActiveUser();
        user.deactivate();
        assertEquals(UserStatus.INACTIVE, user.getStatus());
    }

    @Test
    void suspend_shouldSetStatusToSuspended() {
        User user = createActiveUser();
        user.suspend();
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
    }

    @Test
    void isActive_shouldReturnTrueOnlyForActiveStatus() {
        User user = createActiveUser();
        assertTrue(user.isActive());

        user.suspend();
        assertFalse(user.isActive());

        user.deactivate();
        assertFalse(user.isActive());
    }

    @Test
    void isLocalAuth_shouldReturnTrueForLocalProvider() {
        User local = User.register("local@test.com", "hash", "Local");
        assertTrue(local.isLocalAuth());

        User oauth = User.registerOAuth("oauth@test.com", "OAuth",
                AuthProvider.GITHUB, "id", null);
        assertFalse(oauth.isLocalAuth());
    }

    @Test
    void assignOrganization_shouldSetOrgId() {
        User user = createActiveUser();
        UUID orgId = UUID.randomUUID();

        user.assignOrganization(orgId);

        assertEquals(orgId, user.getOrgId());
    }

    @Test
    void reconstitute_shouldRecreateUserFromPersistedData() {
        UUID id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Instant now = Instant.now();

        User user = User.reconstitute(
                id, orgId, "user@test.com", "hash",
                "Full Name", "https://avatar.url",
                UserRole.ADMIN, UserStatus.ACTIVE, AuthProvider.LOCAL,
                null, "en-US", "UTC",
                "REF12345", true, now, now, now
        );

        assertEquals(id, user.getId());
        assertEquals(orgId, user.getOrgId());
        assertEquals("user@test.com", user.getEmail());
        assertEquals("hash", user.getPasswordHash());
        assertEquals("Full Name", user.getFullName());
        assertEquals("https://avatar.url", user.getAvatarUrl());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(AuthProvider.LOCAL, user.getAuthProvider());
        assertEquals("en-US", user.getLocale());
        assertEquals("UTC", user.getTimezone());
        assertEquals("REF12345", user.getReferralCode());
        assertTrue(user.isEmailVerified());
        assertEquals(now, user.getLastLoginAt());
        assertTrue(user.getDomainEvents().isEmpty());
    }

    @Test
    void clearDomainEvents_shouldRemoveAllEvents() {
        User user = User.register("test@test.com", "hash", "Name");
        assertEquals(1, user.getDomainEvents().size());

        user.clearDomainEvents();

        assertTrue(user.getDomainEvents().isEmpty());
    }

    @Test
    void equality_shouldBeBasedOnId() {
        User user1 = User.register("a@test.com", "hash", "A");
        User user2 = User.register("b@test.com", "hash", "B");

        assertNotEquals(user1, user2);
        assertEquals(user1, user1);
        assertNotEquals(user1, null);
    }

    // Helper
    private User createActiveUser() {
        User user = User.register("active@test.com", "hash", "Active User");
        user.verifyEmail();
        return user;
    }
}
