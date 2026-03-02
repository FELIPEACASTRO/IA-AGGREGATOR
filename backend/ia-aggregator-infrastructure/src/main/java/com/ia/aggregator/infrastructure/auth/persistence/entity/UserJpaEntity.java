package com.ia.aggregator.infrastructure.auth.persistence.entity;

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

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String status;

    @Column(name = "auth_provider", nullable = false)
    private String authProvider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(nullable = false)
    private String locale = "pt-BR";

    @Column(nullable = false)
    private String timezone = "America/Sao_Paulo";

    @Column(name = "referral_code", unique = true)
    private String referralCode;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

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
    public String getAvatarUrl() { return avatarUrl; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getAuthProvider() { return authProvider; }
    public String getProviderUserId() { return providerUserId; }
    public String getLocale() { return locale; }
    public String getTimezone() { return timezone; }
    public String getReferralCode() { return referralCode; }
    public boolean isEmailVerified() { return emailVerified; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setPersonalOrgId(UUID personalOrgId) { this.personalOrgId = personalOrgId; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setRole(String role) { this.role = role; }
    public void setStatus(String status) { this.status = status; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
    public void setLocale(String locale) { this.locale = locale; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
