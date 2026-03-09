package com.ia.aggregator.domain.execution;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A workflow definition with steps, triggers, and configuration.
 */
public record Workflow(
        UUID id,
        UUID orgId,
        UUID createdBy,
        String name,
        String description,
        List<WorkflowStep> steps,
        TriggerConfig trigger,
        WorkflowStatus status,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt
) {
    public enum WorkflowStatus {
        DRAFT, ACTIVE, PAUSED, ARCHIVED
    }

    public record TriggerConfig(
            TriggerType type,
            String cronExpression,
            String webhookPath,
            String eventType,
            Map<String, String> config
    ) {}

    public enum TriggerType {
        MANUAL, WEBHOOK, CRON, EMAIL, FILE_UPLOAD, INTERNAL_EVENT
    }
}
