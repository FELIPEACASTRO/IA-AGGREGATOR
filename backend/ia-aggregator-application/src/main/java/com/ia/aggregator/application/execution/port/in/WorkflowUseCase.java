package com.ia.aggregator.application.execution.port.in;

import com.ia.aggregator.domain.execution.Workflow;
import com.ia.aggregator.domain.execution.WorkflowRun;

import java.util.List;
import java.util.UUID;

/**
 * Use case for workflow management and execution.
 */
public interface WorkflowUseCase {

    Workflow create(Workflow workflow);

    Workflow getById(UUID workflowId);

    List<Workflow> listByOrg(UUID orgId);

    Workflow update(Workflow workflow);

    Workflow activate(UUID workflowId);

    Workflow pause(UUID workflowId);

    void delete(UUID workflowId);

    WorkflowRun trigger(UUID workflowId, UUID triggeredBy);

    WorkflowRun getRunById(UUID runId);

    List<WorkflowRun> listRuns(UUID workflowId, int page, int size);

    WorkflowRun cancelRun(UUID runId);

    WorkflowRun approveStep(UUID runId, String stepId, UUID approverId);

    WorkflowRun rejectStep(UUID runId, String stepId, UUID approverId, String reason);
}
