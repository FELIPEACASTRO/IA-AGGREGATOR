package com.ia.aggregator.application.artifact.port.in;

import com.ia.aggregator.domain.artifact.Artifact;
import com.ia.aggregator.domain.artifact.ArtifactVersion;

import java.util.List;
import java.util.UUID;

/**
 * Use case for artifact management (create, version, share, export).
 */
public interface ArtifactUseCase {

    Artifact create(UUID orgId, UUID userId, String title, Artifact.ArtifactType type,
                    String content, String mimeType);

    Artifact getById(UUID artifactId);

    List<Artifact> listByConversation(UUID conversationId);

    List<Artifact> listByOrg(UUID orgId, int page, int size);

    Artifact update(UUID artifactId, String content, String changeDescription, UUID userId);

    List<ArtifactVersion> getVersionHistory(UUID artifactId);

    ArtifactVersion getVersion(UUID artifactId, int versionNumber);

    Artifact publish(UUID artifactId);

    Artifact archive(UUID artifactId);

    ShareResult share(UUID artifactId, boolean isPublic);

    byte[] export(UUID artifactId, ExportFormat format);

    void delete(UUID artifactId);

    record ShareResult(String shareUrl, String shareToken, boolean isPublic) {}

    enum ExportFormat { TXT, MARKDOWN, HTML, PDF }
}
