package com.ia.aggregator.presentation.compliance;

import com.ia.aggregator.application.compliance.port.in.ComplianceUseCase;
import com.ia.aggregator.domain.compliance.ConsentRecord;
import com.ia.aggregator.domain.compliance.DataErasureRequest;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LGPD/GDPR compliance endpoints.
 */
@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private final ComplianceUseCase complianceUseCase;

    public ComplianceController(ComplianceUseCase complianceUseCase) {
        this.complianceUseCase = complianceUseCase;
    }

    // Consent management
    @PostMapping("/consent")
    public ResponseEntity<ConsentRecord> grantConsent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "unknown") String ip) {
        ConsentRecord.ConsentType type =
                ConsentRecord.ConsentType.valueOf(body.get("type").toUpperCase());
        return ResponseEntity.ok(complianceUseCase.grantConsent(
                user.getUserId(), user.getOrgId(), type, body.get("purpose"), ip
        ));
    }

    @DeleteMapping("/consent/{type}")
    public ResponseEntity<ConsentRecord> revokeConsent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String type) {
        return ResponseEntity.ok(complianceUseCase.revokeConsent(
                user.getUserId(), ConsentRecord.ConsentType.valueOf(type.toUpperCase())
        ));
    }

    @GetMapping("/consent")
    public ResponseEntity<List<ConsentRecord>> getConsents(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(complianceUseCase.getConsents(user.getUserId()));
    }

    // Right to erasure
    @PostMapping("/erasure")
    public ResponseEntity<DataErasureRequest> requestErasure(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(complianceUseCase.requestErasure(
                user.getUserId(), user.getOrgId(), body.get("reason")
        ));
    }

    @GetMapping("/erasure/{id}")
    @RequiresPermission(Permission.AUDIT_READ)
    public ResponseEntity<DataErasureRequest> getErasureRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(complianceUseCase.getErasureRequest(id));
    }

    @GetMapping("/erasure")
    @RequiresPermission(Permission.AUDIT_READ)
    public ResponseEntity<List<DataErasureRequest>> listErasureRequests(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(complianceUseCase.listErasureRequests(
                user.getOrgId(), page, size));
    }

    @PostMapping("/erasure/{id}/process")
    @RequiresPermission(Permission.ADMIN_FULL)
    public ResponseEntity<DataErasureRequest> processErasure(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(complianceUseCase.processErasure(id, user.getUserId()));
    }

    // Data portability
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportData(
            @AuthenticationPrincipal AuthenticatedUser user) {
        byte[] data = complianceUseCase.exportUserData(user.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=user-data-export.json")
                .body(data);
    }
}
