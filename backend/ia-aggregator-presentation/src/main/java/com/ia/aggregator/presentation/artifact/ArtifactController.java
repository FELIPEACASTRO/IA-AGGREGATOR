package com.ia.aggregator.presentation.artifact;

import com.ia.aggregator.application.artifact.port.in.ArtifactUseCase;
import com.ia.aggregator.domain.artifact.Artifact;
import com.ia.aggregator.domain.artifact.ArtifactVersion;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Artifact Studio endpoints.
 */
@RestController
@RequestMapping("/api/v1/artifacts")
public class ArtifactController {

    private final ArtifactUseCase artifactUseCase;

    public ArtifactController(ArtifactUseCase artifactUseCase) {
        this.artifactUseCase = artifactUseCase;
    }

    @PostMapping
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Artifact> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(artifactUseCase.create(
                user.getOrgId(), user.getUserId(),
                body.get("title"),
                Artifact.ArtifactType.valueOf(body.get("type").toUpperCase()),
                body.get("content"),
                body.get("mimeType")
        ));
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<Artifact> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(artifactUseCase.getById(id));
    }

    @GetMapping
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<Artifact>> listByOrg(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(artifactUseCase.listByOrg(user.getOrgId(), page, size));
    }

    @PutMapping("/{id}")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Artifact> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(artifactUseCase.update(
                id, body.get("content"), body.get("changeDescription"),
                user.getUserId()
        ));
    }

    @GetMapping("/{id}/versions")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<List<ArtifactVersion>> getVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(artifactUseCase.getVersionHistory(id));
    }

    @GetMapping("/{id}/versions/{version}")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<ArtifactVersion> getVersion(@PathVariable UUID id,
                                                       @PathVariable int version) {
        return ResponseEntity.ok(artifactUseCase.getVersion(id, version));
    }

    @PostMapping("/{id}/publish")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Artifact> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(artifactUseCase.publish(id));
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Artifact> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(artifactUseCase.archive(id));
    }

    @PostMapping("/{id}/share")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<ArtifactUseCase.ShareResult> share(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        boolean isPublic = body.getOrDefault("public", false);
        return ResponseEntity.ok(artifactUseCase.share(id, isPublic));
    }

    @GetMapping("/{id}/export")
    @RequiresPermission(Permission.CHAT_READ)
    public ResponseEntity<byte[]> export(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "MARKDOWN") String format) {
        ArtifactUseCase.ExportFormat exportFormat =
                ArtifactUseCase.ExportFormat.valueOf(format.toUpperCase());
        byte[] data = artifactUseCase.export(id, exportFormat);

        String contentType = switch (exportFormat) {
            case TXT -> "text/plain";
            case MARKDOWN -> "text/markdown";
            case HTML -> "text/html";
            case PDF -> "application/pdf";
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=artifact." +
                        format.toLowerCase())
                .body(data);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        artifactUseCase.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
