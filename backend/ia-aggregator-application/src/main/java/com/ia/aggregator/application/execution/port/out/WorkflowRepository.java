package com.ia.aggregator.application.execution.port.out;

import com.ia.aggregator.domain.execution.Workflow;
import com.ia.aggregator.domain.execution.WorkflowRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for workflow and run persistence.
 */
public interface WorkflowRepository {

    void save(Workflow workflow);

    Optional<Workflow> findById(UUID id);

    List<Workflow> findByOrg(UUID orgId);

    void delete(UUID id);

    void saveRun(WorkflowRun run);

    Optional<WorkflowRun> findRunById(UUID id);

    List<WorkflowRun> findRunsByWorkflow(UUID workflowId, int offset, int limit);
}
