package com.ia.aggregator.domain.artifact;

import java.time.Instant;
import java.util.UUID;

/**
 * A versioned snapshot of an artifact's content.
 */
public record ArtifactVersion(
        UUID id,
        UUID artifactId,
        int versionNumber,
        String content,
        String changeDescription,
        UUID changedBy,
        Instant createdAt
) {}
