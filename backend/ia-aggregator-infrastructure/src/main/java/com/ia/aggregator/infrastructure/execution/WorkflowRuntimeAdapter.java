package com.ia.aggregator.infrastructure.execution;

import com.ia.aggregator.application.execution.port.out.WorkflowRuntimePort;
import com.ia.aggregator.domain.execution.Workflow;
import com.ia.aggregator.domain.execution.WorkflowRun;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

/**
 * Stub (no-op) implementation of WorkflowRuntimePort.
 * TODO: Replace with real workflow runtime (e.g., Temporal) implementation.
 */
@Component
public class WorkflowRuntimeAdapter implements WorkflowRuntimePort {

    @Override
    public WorkflowRun submit(Workflow workflow, UUID triggeredBy) {
        return new WorkflowRun(
                UUID.randomUUID(),
                workflow.id(),
                workflow.orgId(),
                triggeredBy,
                WorkflowRun.RunStatus.PENDING,
                null,
                Collections.emptyList(),
                Collections.emptyMap(),
                null,
                Instant.now(),
                null
        );
    }

    @Override
    public void cancel(UUID runId) {
        // no-op
    }

    @Override
    public void signal(UUID runId, String signalName, Object payload) {
        // no-op
    }

    @Override
    public WorkflowRun queryState(UUID runId) {
        return new WorkflowRun(
                runId,
                null,
                null,
                null,
                WorkflowRun.RunStatus.PENDING,
                null,
                Collections.emptyList(),
                Collections.emptyMap(),
                null,
                Instant.now(),
                null
        );
    }
}
