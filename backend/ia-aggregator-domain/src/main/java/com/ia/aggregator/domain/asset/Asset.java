package com.ia.aggregator.domain.asset;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A reusable asset in the library (prompt, template, system instruction, etc.).
 */
public record Asset(
        UUID id,
        UUID orgId,
        UUID createdBy,
        String name,
        String description,
        AssetType type,
        AssetScope scope,
        String content,
        Map<String, String> variables,
        Map<String, String> metadata,
        long usageCount,
        double averageRating,
        boolean published,
        Instant createdAt,
        Instant updatedAt
) {
    public enum AssetType {
        PROMPT, TEMPLATE, SYSTEM_INSTRUCTION, AGENT, PLAYBOOK,
        MODEL_PRESET, WORKFLOW
    }

    public enum AssetScope {
        PERSONAL, TEAM, ORGANIZATION
    }
}
