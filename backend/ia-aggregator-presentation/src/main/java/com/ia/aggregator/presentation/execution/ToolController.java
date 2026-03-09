package com.ia.aggregator.presentation.execution;

import com.ia.aggregator.application.execution.port.in.ToolRegistryUseCase;
import com.ia.aggregator.domain.execution.ToolDefinition;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tool registry management endpoints.
 */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolRegistryUseCase toolRegistryUseCase;

    public ToolController(ToolRegistryUseCase toolRegistryUseCase) {
        this.toolRegistryUseCase = toolRegistryUseCase;
    }

    @PostMapping
    @RequiresPermission(Permission.PROVIDER_CONFIG_MANAGE)
    public ResponseEntity<ToolDefinition> register(@RequestBody ToolDefinition tool) {
        return ResponseEntity.ok(toolRegistryUseCase.register(tool));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<ToolDefinition> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(toolRegistryUseCase.getById(id));
    }

    @GetMapping
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<ToolDefinition>> listAll(
            @RequestParam(required = false) String scope) {
        if (scope != null) {
            return ResponseEntity.ok(toolRegistryUseCase.listByScope(
                    ToolDefinition.ToolScope.valueOf(scope.toUpperCase())));
        }
        return ResponseEntity.ok(toolRegistryUseCase.listAll());
    }

    @PostMapping("/{id}/invoke")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<ToolRegistryUseCase.ToolInvocationResult> invoke(
            @PathVariable UUID id,
            @RequestBody String inputJson) {
        return ResponseEntity.ok(toolRegistryUseCase.invoke(id, inputJson));
    }

    @PostMapping("/{id}/disable")
    @RequiresPermission(Permission.PROVIDER_CONFIG_MANAGE)
    public ResponseEntity<Map<String, String>> disable(@PathVariable UUID id) {
        toolRegistryUseCase.disable(id);
        return ResponseEntity.ok(Map.of("status", "disabled"));
    }

    @PostMapping("/{id}/enable")
    @RequiresPermission(Permission.PROVIDER_CONFIG_MANAGE)
    public ResponseEntity<Map<String, String>> enable(@PathVariable UUID id) {
        toolRegistryUseCase.enable(id);
        return ResponseEntity.ok(Map.of("status", "enabled"));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.PROVIDER_CONFIG_MANAGE)
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        toolRegistryUseCase.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
