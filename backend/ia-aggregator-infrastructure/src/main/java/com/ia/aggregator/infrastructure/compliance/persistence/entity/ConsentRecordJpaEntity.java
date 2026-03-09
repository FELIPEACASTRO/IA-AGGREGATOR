package com.ia.aggregator.infrastructure.compliance.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_records", schema = "auth")
public class ConsentRecordJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "consent_type", nullable = false)
    private String consentType;

    @Column
    private String version;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(name = "document_hash")
    private String documentHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ConsentRecordJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getConsentType() { return consentType; }
    public String getVersion() { return version; }
    public boolean isGranted() { return granted; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getDocumentUrl() { return documentUrl; }
    public String getDocumentHash() { return documentHash; }
    public Instant getCreatedAt() { return createdAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setConsentType(String consentType) { this.consentType = consentType; }
    public void setVersion(String version) { this.version = version; }
    public void setGranted(boolean granted) { this.granted = granted; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }
    public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
