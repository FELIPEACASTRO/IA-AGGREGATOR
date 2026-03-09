package com.ia.aggregator.presentation.auth;

import com.ia.aggregator.application.auth.port.in.ApiKeyUseCase;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API key management endpoints.
 */
@RestController
@RequestMapping("/api/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyUseCase apiKeyUseCase;

    public ApiKeyController(ApiKeyUseCase apiKeyUseCase) {
        this.apiKeyUseCase = apiKeyUseCase;
    }

    @PostMapping
    @RequiresPermission(Permission.API_KEY_CREATE)
    public ResponseEntity<ApiKeyUseCase.ApiKeyResult> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        List<String> scopes = (List<String>) body.getOrDefault("scopes", List.of());
        return ResponseEntity.ok(apiKeyUseCase.create(user.getUserId(), name, scopes));
    }

    @GetMapping
    @RequiresPermission(Permission.API_KEY_READ)
    public ResponseEntity<List<ApiKeyUseCase.ApiKeyInfo>> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(apiKeyUseCase.listByUser(user.getUserId()));
    }

    @DeleteMapping("/{keyId}")
    @RequiresPermission(Permission.API_KEY_REVOKE)
    public ResponseEntity<Map<String, String>> revoke(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID keyId) {
        apiKeyUseCase.revoke(user.getUserId(), keyId);
        return ResponseEntity.ok(Map.of("status", "revoked"));
    }
}
