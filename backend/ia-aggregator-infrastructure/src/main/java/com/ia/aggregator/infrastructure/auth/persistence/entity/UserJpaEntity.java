package com.ia.aggregator.infrastructure.auth.persistence.entity;

import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import com.ia.aggregator.infrastructure.auth.persistence.converter.AuthProviderConverter;
import com.ia.aggregator.infrastructure.auth.persistence.converter.UserRoleConverter;
import com.ia.aggregator.infrastructure.auth.persistence.converter.UserStatusConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "auth")
public class UserJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "personal_org_id")
    private UUID personalOrgId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "phone")
    private String phone;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Convert(converter = UserRoleConverter.class)
    @Column(nullable = false)
    private UserRole role;

    @Convert(converter = UserStatusConverter.class)
    @Column(nullable = false)
    private UserStatus status;

    @Convert(converter = AuthProviderConverter.class)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(nullable = false)
    private String locale = "pt-BR";

    @Column(nullable = false)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "referral_code", unique = true)
    private String referralCode;

    @Column(name = "referred_by_user_id")
    private UUID referredByUserId;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "cpf_hash")
    private String cpfHash;

    @Column(name = "data_export_requested_at")
    private Instant dataExportRequestedAt;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "onboarding_step", nullable = false)
    private int onboardingStep = 0;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UserJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getPersonalOrgId() { return personalOrgId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPhone() { return phone; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public String getProviderUserId() { return providerUserId; }
    public String getLocale() { return locale; }
    public String getTimezone() { return timezone; }
    public String getReferralCode() { return referralCode; }
    public UUID getReferredByUserId() { return referredByUserId; }
    public boolean isEmailVerified() { return emailVerified; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public String getLastLoginIp() { return lastLoginIp; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public Instant getLockedUntil() { return lockedUntil; }
    public String getCpfHash() { return cpfHash; }
    public Instant getDataExportRequestedAt() { return dataExportRequestedAt; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public int getOnboardingStep() { return onboardingStep; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setPersonalOrgId(UUID personalOrgId) { this.personalOrgId = personalOrgId; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }
    public void setRole(UserRole role) { this.role = role; }
    public void setStatus(UserStatus status) { this.status = status; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
    public void setLocale(String locale) { this.locale = locale; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public void setReferredByUserId(UUID referredByUserId) { this.referredByUserId = referredByUserId; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setEmailVerifiedAt(Instant emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public void setFailedLoginCount(int failedLoginCount) { this.failedLoginCount = failedLoginCount; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public void setCpfHash(String cpfHash) { this.cpfHash = cpfHash; }
    public void setDataExportRequestedAt(Instant dataExportRequestedAt) { this.dataExportRequestedAt = dataExportRequestedAt; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
    public void setOnboardingStep(int onboardingStep) { this.onboardingStep = onboardingStep; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
