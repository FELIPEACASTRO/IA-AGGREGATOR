package com.ia.aggregator.domain.execution;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A single execution instance of a workflow.
 */
public record WorkflowRun(
        UUID id,
        UUID workflowId,
        UUID orgId,
        UUID triggeredBy,
        RunStatus status,
        String currentStepId,
        List<StepExecution> stepHistory,
        Map<String, Object> context,
        String errorMessage,
        Instant startedAt,
        Instant completedAt
) {
    public enum RunStatus {
        PENDING, RUNNING, WAITING_APPROVAL, PAUSED,
        COMPLETED, FAILED, CANCELED, TIMED_OUT
    }

    public record StepExecution(
            String stepId,
            StepExecutionStatus status,
            Map<String, Object> input,
            Map<String, Object> output,
            String errorMessage,
            long durationMs,
            int attempt,
            Instant startedAt,
            Instant completedAt
    ) {}

    public enum StepExecutionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, SKIPPED, RETRYING
    }
}
