package com.ia.aggregator.domain.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit event for compliance tracking.
 *
 * @param id         Unique event identifier
 * @param timestamp  When the event occurred
 * @param actorId    User who performed the action (null for system events)
 * @param actorEmail Actor email for display
 * @param orgId      Organization context
 * @param action     What action was performed (e.g., "user.login", "ai.chat.create")
 * @param resource   Resource type (e.g., "user", "conversation", "api-key")
 * @param resourceId Identifier of the affected resource
 * @param outcome    Result: SUCCESS, FAILURE, DENIED
 * @param ipAddress  Client IP address
 * @param userAgent  Client user agent
 * @param metadata   Additional context (model used, tokens consumed, etc.)
 */
public record AuditEvent(
        UUID id,
        Instant timestamp,
        UUID actorId,
        String actorEmail,
        UUID orgId,
        String action,
        String resource,
        String resourceId,
        AuditOutcome outcome,
        String ipAddress,
        String userAgent,
        Map<String, String> metadata
) {
    public AuditEvent {
        if (id == null) id = UUID.randomUUID();
        if (timestamp == null) timestamp = Instant.now();
        if (metadata == null) metadata = Map.of();
    }

    public enum AuditOutcome {
        SUCCESS, FAILURE, DENIED
    }

    public static AuditEvent success(UUID actorId, String actorEmail, UUID orgId,
                                      String action, String resource, String resourceId,
                                      String ipAddress, Map<String, String> metadata) {
        return new AuditEvent(UUID.randomUUID(), Instant.now(), actorId, actorEmail, orgId,
                action, resource, resourceId, AuditOutcome.SUCCESS, ipAddress, null, metadata);
    }

    public static AuditEvent denied(UUID actorId, String actorEmail, UUID orgId,
                                     String action, String resource, String ipAddress) {
        return new AuditEvent(UUID.randomUUID(), Instant.now(), actorId, actorEmail, orgId,
                action, resource, null, AuditOutcome.DENIED, ipAddress, null, Map.of());
    }
}
