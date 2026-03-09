package com.ia.aggregator.presentation.asset;

import com.ia.aggregator.application.asset.port.in.AssetUseCase;
import com.ia.aggregator.application.asset.port.in.AgentUseCase;
import com.ia.aggregator.domain.asset.Asset;
import com.ia.aggregator.domain.asset.AgentDefinition;
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
 * Asset Library and Agent Builder endpoints.
 */
@RestController
@RequestMapping("/api/v1/library")
public class AssetLibraryController {

    private final AssetUseCase assetUseCase;
    private final AgentUseCase agentUseCase;

    public AssetLibraryController(AssetUseCase assetUseCase, AgentUseCase agentUseCase) {
        this.assetUseCase = assetUseCase;
        this.agentUseCase = agentUseCase;
    }

    // ===== Asset CRUD =====

    @SuppressWarnings("unchecked")
    @PostMapping("/assets")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Asset> createAsset(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(assetUseCase.create(
                user.getOrgId(), user.getUserId(),
                (String) body.get("name"),
                (String) body.get("description"),
                Asset.AssetType.valueOf(((String) body.get("type")).toUpperCase()),
                Asset.AssetScope.valueOf(((String) body.get("scope")).toUpperCase()),
                (String) body.get("content"),
                (Map<String, String>) body.get("variables")
        ));
    }

    @GetMapping("/assets/{id}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<Asset> getAsset(@PathVariable UUID id) {
        return ResponseEntity.ok(assetUseCase.getById(id));
    }

    @GetMapping("/assets")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<Asset>> listAssets(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Asset.AssetScope s = scope != null ? Asset.AssetScope.valueOf(scope.toUpperCase()) : null;
        Asset.AssetType t = type != null ? Asset.AssetType.valueOf(type.toUpperCase()) : null;
        return ResponseEntity.ok(assetUseCase.listByOrg(user.getOrgId(), s, t, page, size));
    }

    @GetMapping("/assets/search")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<Asset>> searchAssets(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(assetUseCase.search(user.getOrgId(), q, page, size));
    }

    @PostMapping("/assets/{id}/render")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Map<String, String>> renderTemplate(
            @PathVariable UUID id,
            @RequestBody Map<String, String> variables) {
        String rendered = assetUseCase.renderTemplate(id, variables);
        return ResponseEntity.ok(Map.of("rendered", rendered));
    }

    @GetMapping("/assets/{id}/metrics")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<AssetUseCase.AssetMetrics> getAssetMetrics(@PathVariable UUID id) {
        return ResponseEntity.ok(assetUseCase.getMetrics(id));
    }

    @DeleteMapping("/assets/{id}")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Map<String, String>> deleteAsset(@PathVariable UUID id) {
        assetUseCase.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    // ===== Agent CRUD =====

    @PostMapping("/agents")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<AgentDefinition> createAgent(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody AgentDefinition agent) {
        AgentDefinition withOrg = new AgentDefinition(
                null, user.getOrgId(), agent.name(), agent.description(),
                agent.identity(), agent.instructions(), agent.defaultModel(),
                agent.knowledgeBaseId(), agent.permittedTools(),
                agent.maxBudgetUsd(), agent.requiresApproval(),
                agent.category(), agent.metadata()
        );
        return ResponseEntity.ok(agentUseCase.create(withOrg));
    }

    @GetMapping("/agents/{id}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<AgentDefinition> getAgent(@PathVariable UUID id) {
        return ResponseEntity.ok(agentUseCase.getById(id));
    }

    @GetMapping("/agents")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<AgentDefinition>> listAgents(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String category) {
        if (category != null) {
            return ResponseEntity.ok(agentUseCase.listByCategory(
                    user.getOrgId(),
                    AgentDefinition.AgentCategory.valueOf(category.toUpperCase())
            ));
        }
        return ResponseEntity.ok(agentUseCase.listByOrg(user.getOrgId()));
    }

    @GetMapping("/agents/prebuilt")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<AgentDefinition>> listPrebuiltAgents() {
        return ResponseEntity.ok(agentUseCase.listPrebuilt());
    }

    @PutMapping("/agents/{id}")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<AgentDefinition> updateAgent(
            @PathVariable UUID id,
            @RequestBody AgentDefinition agent) {
        AgentDefinition withId = new AgentDefinition(
                id, agent.orgId(), agent.name(), agent.description(),
                agent.identity(), agent.instructions(), agent.defaultModel(),
                agent.knowledgeBaseId(), agent.permittedTools(),
                agent.maxBudgetUsd(), agent.requiresApproval(),
                agent.category(), agent.metadata()
        );
        return ResponseEntity.ok(agentUseCase.update(withId));
    }

    @DeleteMapping("/agents/{id}")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Map<String, String>> deleteAgent(@PathVariable UUID id) {
        agentUseCase.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
