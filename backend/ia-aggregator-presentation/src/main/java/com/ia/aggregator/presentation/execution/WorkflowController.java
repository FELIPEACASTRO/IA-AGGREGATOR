package com.ia.aggregator.presentation.execution;

import com.ia.aggregator.application.execution.port.in.WorkflowUseCase;
import com.ia.aggregator.domain.execution.Workflow;
import com.ia.aggregator.domain.execution.WorkflowRun;
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
 * Workflow management and execution endpoints.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowUseCase workflowUseCase;

    public WorkflowController(WorkflowUseCase workflowUseCase) {
        this.workflowUseCase = workflowUseCase;
    }

    @PostMapping
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Workflow> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Workflow workflow) {
        Workflow withOrg = new Workflow(
                null, user.getOrgId(), user.getUserId(),
                workflow.name(), workflow.description(), workflow.steps(),
                workflow.trigger(), null, workflow.metadata(), null, null
        );
        return ResponseEntity.ok(workflowUseCase.create(withOrg));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<Workflow> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowUseCase.getById(id));
    }

    @GetMapping
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<Workflow>> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(workflowUseCase.listByOrg(user.getOrgId()));
    }

    @PostMapping("/{id}/activate")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Workflow> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowUseCase.activate(id));
    }

    @PostMapping("/{id}/pause")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Workflow> pause(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowUseCase.pause(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        workflowUseCase.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PostMapping("/{id}/trigger")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<WorkflowRun> trigger(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(workflowUseCase.trigger(id, user.getUserId()));
    }

    @GetMapping("/{id}/runs")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<WorkflowRun>> listRuns(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(workflowUseCase.listRuns(id, page, size));
    }

    @GetMapping("/runs/{runId}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<WorkflowRun> getRun(@PathVariable UUID runId) {
        return ResponseEntity.ok(workflowUseCase.getRunById(runId));
    }

    @PostMapping("/runs/{runId}/cancel")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<WorkflowRun> cancelRun(@PathVariable UUID runId) {
        return ResponseEntity.ok(workflowUseCase.cancelRun(runId));
    }

    @PostMapping("/runs/{runId}/steps/{stepId}/approve")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<WorkflowRun> approveStep(
            @PathVariable UUID runId, @PathVariable String stepId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(workflowUseCase.approveStep(runId, stepId, user.getUserId()));
    }
}
