package com.ia.aggregator.domain.execution;

import java.util.Map;

/**
 * A single step in a workflow definition.
 */
public record WorkflowStep(
        String stepId,
        String name,
        StepType type,
        Map<String, Object> config,
        String conditionExpression,
        int maxRetries,
        long retryBackoffMs,
        long timeoutMs,
        String nextStepId,
        String errorStepId
) {
    public enum StepType {
        AI_PROMPT, HTTP_CALL, DATA_TRANSFORM, CONDITIONAL,
        LOOP, DELAY, INTEGRATION, HUMAN_APPROVAL
    }
}
