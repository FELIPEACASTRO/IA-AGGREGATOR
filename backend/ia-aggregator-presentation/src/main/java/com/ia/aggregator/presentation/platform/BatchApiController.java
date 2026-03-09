package com.ia.aggregator.presentation.platform;

import com.ia.aggregator.application.platform.port.in.BatchApiUseCase;
import com.ia.aggregator.domain.platform.BatchJob;
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
 * Batch API endpoints for bulk request processing.
 */
@RestController
@RequestMapping("/api/v1/platform/batch")
public class BatchApiController {

    private final BatchApiUseCase batchApiUseCase;

    public BatchApiController(BatchApiUseCase batchApiUseCase) {
        this.batchApiUseCase = batchApiUseCase;
    }

    @SuppressWarnings("unchecked")
    @PostMapping
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<BatchJob> submit(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        String webhookUrl = (String) body.get("webhookUrl");
        List<Map<String, Object>> requests = (List<Map<String, Object>>) body.get("requests");
        return ResponseEntity.ok(batchApiUseCase.submit(user.getOrgId(), webhookUrl, requests));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<BatchJob> getStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(batchApiUseCase.getStatus(id));
    }

    @GetMapping
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<BatchJob>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(batchApiUseCase.listByOrg(user.getOrgId(), page, size));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<BatchJob> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(batchApiUseCase.cancel(id));
    }
}
