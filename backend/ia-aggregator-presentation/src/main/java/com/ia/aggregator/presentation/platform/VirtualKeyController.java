package com.ia.aggregator.presentation.platform;

import com.ia.aggregator.application.platform.port.in.VirtualKeyUseCase;
import com.ia.aggregator.domain.platform.VirtualKey;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Virtual API key management for developer platform.
 */
@RestController
@RequestMapping("/api/v1/platform/keys")
public class VirtualKeyController {

    private final VirtualKeyUseCase virtualKeyUseCase;

    public VirtualKeyController(VirtualKeyUseCase virtualKeyUseCase) {
        this.virtualKeyUseCase = virtualKeyUseCase;
    }

    @SuppressWarnings("unchecked")
    @PostMapping
    @RequiresPermission(Permission.API_KEY_CREATE)
    public ResponseEntity<VirtualKeyUseCase.VirtualKeyResult> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        List<String> models = (List<String>) body.get("allowedModels");
        List<String> capabilities = (List<String>) body.get("allowedCapabilities");
        int rpm = body.containsKey("rateLimitRpm") ?
                ((Number) body.get("rateLimitRpm")).intValue() : 60;
        double budget = body.containsKey("budgetLimitUsd") ?
                ((Number) body.get("budgetLimitUsd")).doubleValue() : 100.0;
        Instant expiresAt = body.containsKey("expiresAt") ?
                Instant.parse((String) body.get("expiresAt")) : null;

        return ResponseEntity.ok(virtualKeyUseCase.create(
                user.getOrgId(), user.getUserId(), name,
                models, capabilities, rpm, budget, expiresAt
        ));
    }

    @GetMapping
    @RequiresPermission(Permission.API_KEY_READ)
    public ResponseEntity<List<VirtualKey>> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(virtualKeyUseCase.listByOrg(user.getOrgId()));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.API_KEY_READ)
    public ResponseEntity<VirtualKey> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(virtualKeyUseCase.getById(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.API_KEY_REVOKE)
    public ResponseEntity<Map<String, String>> revoke(@PathVariable UUID id) {
        virtualKeyUseCase.revoke(id);
        return ResponseEntity.ok(Map.of("status", "revoked"));
    }
}
