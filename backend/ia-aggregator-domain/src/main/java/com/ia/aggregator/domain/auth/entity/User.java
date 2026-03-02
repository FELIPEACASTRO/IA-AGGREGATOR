package com.ia.aggregator.domain.auth.entity;

import com.ia.aggregator.domain.auth.event.UserRegisteredEvent;
import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import com.ia.aggregator.domain.shared.entity.BaseEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Core User aggregate root.
 * Multi-tenant: every user belongs to a personal organization (orgId).
 */
public class User extends BaseEntity {

    private UUID orgId;
    private String email;
    private String passwordHash;
    private String fullName;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private AuthProvider authProvider;
    private String providerUserId;
    private String locale;
    private String timezone;
    private String referralCode;
    private boolean emailVerified;
    private Instant lastLoginAt;
    private int failedLoginCount;
    private Instant lockedUntil;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    // Private constructor — use factory methods
    private User() {
        super();
    }

    /**
     * Register a new user with email/password.
     */
    public static User register(String email, String passwordHash, String fullName) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }

        User user = new User();
        user.email = email.toLowerCase().trim();
        user.passwordHash = passwordHash;
        user.fullName = fullName.trim();
        user.role = UserRole.USER;
        user.status = UserStatus.PENDING_VERIFICATION;
        user.authProvider = AuthProvider.LOCAL;
        user.locale = "pt-BR";
        user.timezone = "America/Sao_Paulo";
        user.referralCode = generateReferralCode();
        user.emailVerified = false;

        user.registerEvent(new UserRegisteredEvent(user.getId(), user.email, user.authProvider));
        return user;
    }

    /**
     * Register via OAuth provider (Google, GitHub).
     */
    public static User registerOAuth(String email, String fullName, AuthProvider provider, String providerUserId, String avatarUrl) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Auth provider is required");
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("Provider user ID is required");
        }

        User user = new User();
        user.email = email.toLowerCase().trim();
        user.fullName = fullName.trim();
        user.authProvider = provider;
        user.providerUserId = providerUserId;
        user.avatarUrl = avatarUrl;
        user.role = UserRole.USER;
        user.status = UserStatus.ACTIVE;
        user.locale = "pt-BR";
        user.timezone = "America/Sao_Paulo";
        user.referralCode = generateReferralCode();
        user.emailVerified = true; // OAuth emails are pre-verified

        user.registerEvent(new UserRegisteredEvent(user.getId(), user.email, user.authProvider));
        return user;
    }

    /**
     * Reconstruct from persistence (no events emitted).
     */
    public static User reconstitute(UUID id, UUID orgId, String email, String passwordHash,
                                     String fullName, String avatarUrl, UserRole role,
                                     UserStatus status, AuthProvider authProvider,
                                     String providerUserId, String locale, String timezone,
                                     String referralCode, boolean emailVerified,
                                     Instant lastLoginAt, int failedLoginCount,
                                     Instant lockedUntil, Instant createdAt, Instant updatedAt) {
        User user = new User();
        user.setId(id);
        user.orgId = orgId;
        user.email = email;
        user.passwordHash = passwordHash;
        user.fullName = fullName;
        user.avatarUrl = avatarUrl;
        user.role = role;
        user.status = status;
        user.authProvider = authProvider;
        user.providerUserId = providerUserId;
        user.locale = locale;
        user.timezone = timezone;
        user.referralCode = referralCode;
        user.emailVerified = emailVerified;
        user.lastLoginAt = lastLoginAt;
        user.failedLoginCount = failedLoginCount;
        user.lockedUntil = lockedUntil;
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);
        return user;
    }

    public void verifyEmail() {
        this.emailVerified = true;
        if (this.status == UserStatus.PENDING_VERIFICATION) {
            this.status = UserStatus.ACTIVE;
        }
        markUpdated();
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        markUpdated();
    }

    public void recordFailedLogin() {
        this.failedLoginCount++;
        if (this.failedLoginCount >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60);
        }
        markUpdated();
    }

    public boolean isLocked() {
        return this.lockedUntil != null && Instant.now().isBefore(this.lockedUntil);
    }

    public void changePassword(String newPasswordHash) {
        if (this.authProvider != AuthProvider.LOCAL) {
            throw new IllegalStateException("Cannot change password for OAuth users");
        }
        this.passwordHash = newPasswordHash;
        markUpdated();
    }

    public void updateProfile(String fullName, String avatarUrl, String locale, String timezone) {
        if (fullName != null && !fullName.isBlank()) this.fullName = fullName;
        if (avatarUrl != null) this.avatarUrl = avatarUrl;
        if (locale != null && !locale.isBlank()) this.locale = locale;
        if (timezone != null && !timezone.isBlank()) this.timezone = timezone;
        markUpdated();
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        markUpdated();
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        markUpdated();
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isLocalAuth() {
        return this.authProvider == AuthProvider.LOCAL;
    }

    public void assignOrganization(UUID orgId) {
        this.orgId = orgId;
        markUpdated();
    }

    private static String generateReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters
    public UUID getOrgId() { return orgId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public String getProviderUserId() { return providerUserId; }
    public String getLocale() { return locale; }
    public String getTimezone() { return timezone; }
    public String getReferralCode() { return referralCode; }
    public boolean isEmailVerified() { return emailVerified; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public Instant getLockedUntil() { return lockedUntil; }
}
