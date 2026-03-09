package com.ia.aggregator.presentation.platform;

import com.ia.aggregator.application.platform.port.in.RequestTracingUseCase;
import com.ia.aggregator.domain.platform.RequestTrace;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Request tracing and observability endpoints.
 */
@RestController
@RequestMapping("/api/v1/platform/traces")
public class RequestTracingController {

    private final RequestTracingUseCase tracingUseCase;

    public RequestTracingController(RequestTracingUseCase tracingUseCase) {
        this.tracingUseCase = tracingUseCase;
    }

    @GetMapping
    @RequiresPermission(Permission.AUDIT_READ)
    public ResponseEntity<List<RequestTrace>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        return ResponseEntity.ok(tracingUseCase.listByOrg(
                user.getOrgId(), from, Instant.now(), page, size));
    }

    @GetMapping("/key/{keyId}")
    @RequiresPermission(Permission.AUDIT_READ)
    public ResponseEntity<List<RequestTrace>> listByKey(
            @PathVariable UUID keyId,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        return ResponseEntity.ok(tracingUseCase.listByVirtualKey(
                keyId, from, Instant.now(), page, size));
    }

    @GetMapping("/summary")
    @RequiresPermission(Permission.AUDIT_READ)
    public ResponseEntity<RequestTracingUseCase.TracingSummary> getSummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "7") int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        return ResponseEntity.ok(tracingUseCase.getSummary(
                user.getOrgId(), from, Instant.now()));
    }
}
