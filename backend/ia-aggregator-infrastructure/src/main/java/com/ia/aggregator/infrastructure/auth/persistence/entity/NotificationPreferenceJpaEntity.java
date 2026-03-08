package com.ia.aggregator.infrastructure.auth.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences", schema = "auth")
public class NotificationPreferenceJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = false;

    @Column(name = "billing_alerts", nullable = false)
    private boolean billingAlerts = true;

    @Column(name = "usage_alerts", nullable = false)
    private boolean usageAlerts = true;

    @Column(name = "credit_low_threshold")
    private Integer creditLowThreshold = 100;

    @Column(name = "product_updates", nullable = false)
    private boolean productUpdates = true;

    @Column(name = "marketing", nullable = false)
    private boolean marketing = false;

    @Column(name = "security_alerts", nullable = false)
    private boolean securityAlerts = true;

    @Column(name = "partner_updates", nullable = false)
    private boolean partnerUpdates = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public boolean isPushEnabled() { return pushEnabled; }
    public boolean isSmsEnabled() { return smsEnabled; }
    public boolean isBillingAlerts() { return billingAlerts; }
    public boolean isUsageAlerts() { return usageAlerts; }
    public Integer getCreditLowThreshold() { return creditLowThreshold; }
    public boolean isProductUpdates() { return productUpdates; }
    public boolean isMarketing() { return marketing; }
    public boolean isSecurityAlerts() { return securityAlerts; }
    public boolean isPartnerUpdates() { return partnerUpdates; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }
    public void setSmsEnabled(boolean smsEnabled) { this.smsEnabled = smsEnabled; }
    public void setBillingAlerts(boolean billingAlerts) { this.billingAlerts = billingAlerts; }
    public void setUsageAlerts(boolean usageAlerts) { this.usageAlerts = usageAlerts; }
    public void setCreditLowThreshold(Integer creditLowThreshold) { this.creditLowThreshold = creditLowThreshold; }
    public void setProductUpdates(boolean productUpdates) { this.productUpdates = productUpdates; }
    public void setMarketing(boolean marketing) { this.marketing = marketing; }
    public void setSecurityAlerts(boolean securityAlerts) { this.securityAlerts = securityAlerts; }
    public void setPartnerUpdates(boolean partnerUpdates) { this.partnerUpdates = partnerUpdates; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
