package com.ia.aggregator.domain.execution;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An agent execution instance with its reasoning loop trace.
 */
public record AgentExecution(
        UUID id,
        UUID agentDefinitionId,
        UUID orgId,
        UUID userId,
        String objective,
        AutonomyLevel autonomyLevel,
        AgentStatus status,
        List<AgentStep> steps,
        double totalCostUsd,
        double maxBudgetUsd,
        String finalAnswer,
        String errorMessage,
        Instant startedAt,
        Instant completedAt
) {
    public enum AgentStatus {
        PLANNING, EXECUTING, WAITING_APPROVAL, DELEGATING,
        COMPLETED, FAILED, BUDGET_EXCEEDED, CANCELED
    }

    public record AgentStep(
            int stepNumber,
            String thought,
            String action,
            String toolName,
            String toolInput,
            String observation,
            double costUsd,
            long durationMs,
            Instant timestamp
    ) {}
}
