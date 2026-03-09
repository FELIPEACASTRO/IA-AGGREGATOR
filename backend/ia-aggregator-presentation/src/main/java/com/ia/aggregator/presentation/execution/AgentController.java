package com.ia.aggregator.presentation.execution;

import com.ia.aggregator.application.execution.port.in.AgentRuntimeUseCase;
import com.ia.aggregator.domain.execution.AgentExecution;
import com.ia.aggregator.domain.execution.AutonomyLevel;
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
 * Agent runtime execution endpoints.
 */
@RestController
@RequestMapping("/api/v1/agents/executions")
public class AgentController {

    private final AgentRuntimeUseCase agentRuntimeUseCase;

    public AgentController(AgentRuntimeUseCase agentRuntimeUseCase) {
        this.agentRuntimeUseCase = agentRuntimeUseCase;
    }

    @PostMapping
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<AgentExecution> execute(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        UUID agentId = UUID.fromString((String) body.get("agentDefinitionId"));
        String objective = (String) body.get("objective");
        AutonomyLevel level = AutonomyLevel.valueOf(
                ((String) body.getOrDefault("autonomyLevel", "L2_CONFIRMATIVE")).toUpperCase());
        double maxBudget = body.containsKey("maxBudgetUsd") ?
                ((Number) body.get("maxBudgetUsd")).doubleValue() : 10.0;

        return ResponseEntity.ok(agentRuntimeUseCase.execute(
                agentId, user.getOrgId(), user.getUserId(),
                objective, level, maxBudget
        ));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<AgentExecution> getExecution(@PathVariable UUID id) {
        return ResponseEntity.ok(agentRuntimeUseCase.getExecution(id));
    }

    @GetMapping
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<AgentExecution>> listExecutions(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(agentRuntimeUseCase.listExecutions(user.getOrgId(), page, size));
    }

    @PostMapping("/{id}/steps/{stepNumber}/approve")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<AgentExecution> approveAction(
            @PathVariable UUID id, @PathVariable int stepNumber) {
        return ResponseEntity.ok(agentRuntimeUseCase.approveAction(id, stepNumber));
    }

    @PostMapping("/{id}/steps/{stepNumber}/reject")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<AgentExecution> rejectAction(
            @PathVariable UUID id, @PathVariable int stepNumber,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(agentRuntimeUseCase.rejectAction(
                id, stepNumber, body.get("reason")));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<AgentExecution> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(agentRuntimeUseCase.cancel(id));
    }

    @PostMapping("/{id}/resume")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<AgentExecution> resume(@PathVariable UUID id) {
        return ResponseEntity.ok(agentRuntimeUseCase.resume(id));
    }
}
