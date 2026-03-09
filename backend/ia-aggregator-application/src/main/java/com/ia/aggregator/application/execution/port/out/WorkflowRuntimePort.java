package com.ia.aggregator.application.execution.port.out;

import com.ia.aggregator.domain.execution.Workflow;
import com.ia.aggregator.domain.execution.WorkflowRun;

import java.util.UUID;

/**
 * Port for durable workflow execution runtime (Temporal/equivalent).
 */
public interface WorkflowRuntimePort {

    /**
     * Submit a workflow for execution.
     */
    WorkflowRun submit(Workflow workflow, UUID triggeredBy);

    /**
     * Cancel a running workflow.
     */
    void cancel(UUID runId);

    /**
     * Signal a waiting workflow step (e.g., human approval).
     */
    void signal(UUID runId, String signalName, Object payload);

    /**
     * Query the current state of a running workflow.
     */
    WorkflowRun queryState(UUID runId);
}
