package com.ia.aggregator.presentation.auth;

import com.ia.aggregator.application.auth.port.out.AuditPort;
import com.ia.aggregator.domain.audit.AuditEvent;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Audit trail query endpoints.
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditPort auditPort;

    public AuditController(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    @GetMapping("/events")
    @RequiresPermission(Permission.AUDIT_READ)
    public ResponseEntity<List<AuditEvent>> getEvents(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "100") int limit) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant to = Instant.now();
        return ResponseEntity.ok(auditPort.findByOrg(user.getOrgId(), from, to, limit));
    }
}
