package com.ia.aggregator.application.auth.port.out;

import com.ia.aggregator.domain.audit.AuditEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Port for audit event persistence and querying.
 */
public interface AuditPort {

    void record(AuditEvent event);

    List<AuditEvent> findByOrg(UUID orgId, Instant from, Instant to, int limit);

    List<AuditEvent> findByActor(UUID actorId, Instant from, Instant to, int limit);

    List<AuditEvent> findByAction(String action, Instant from, Instant to, int limit);
}
