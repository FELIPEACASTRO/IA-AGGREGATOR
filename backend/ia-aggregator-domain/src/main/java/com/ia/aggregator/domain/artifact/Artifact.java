package com.ia.aggregator.domain.artifact;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * An artifact produced by AI interactions (code, text, diagrams, etc.).
 */
public record Artifact(
        UUID id,
        UUID conversationId,
        UUID orgId,
        UUID createdBy,
        String title,
        ArtifactType type,
        String content,
        String mimeType,
        int version,
        ArtifactStatus status,
        String shareToken,
        boolean isPublic,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt
) {
    public enum ArtifactType {
        CODE, MARKDOWN, HTML, RICH_TEXT, MERMAID_DIAGRAM,
        TABLE, FAQ, SCRIPT, REACT_COMPONENT, JSON, CSV
    }

    public enum ArtifactStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
