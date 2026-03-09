package com.ia.aggregator.application.execution.usecase;

import com.ia.aggregator.application.execution.port.in.WorkflowUseCase;
import com.ia.aggregator.application.execution.port.out.WorkflowRepository;
import com.ia.aggregator.application.execution.port.out.WorkflowRuntimePort;
import com.ia.aggregator.domain.execution.Workflow;
import com.ia.aggregator.domain.execution.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class WorkflowUseCaseImpl implements WorkflowUseCase {

    private static final Logger log = LoggerFactory.getLogger(WorkflowUseCaseImpl.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowRuntimePort workflowRuntime;

    public WorkflowUseCaseImpl(WorkflowRepository workflowRepository,
                                WorkflowRuntimePort workflowRuntime) {
        this.workflowRepository = workflowRepository;
        this.workflowRuntime = workflowRuntime;
    }

    @Override
    public Workflow create(Workflow workflow) {
        Instant now = Instant.now();
        Workflow withId = new Workflow(
                workflow.id() != null ? workflow.id() : UUID.randomUUID(),
                workflow.orgId(), workflow.createdBy(), workflow.name(),
                workflow.description(), workflow.steps(), workflow.trigger(),
                Workflow.WorkflowStatus.DRAFT, workflow.metadata(), now, now
        );
        workflowRepository.save(withId);
        log.info("Workflow created: id={}, name={}", withId.id(), withId.name());
        return withId;
    }

    @Override
    public Workflow getById(UUID workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new NoSuchElementException("Workflow not found: " + workflowId));
    }

    @Override
    public List<Workflow> listByOrg(UUID orgId) {
        return workflowRepository.findByOrg(orgId);
    }

    @Override
    public Workflow update(Workflow workflow) {
        Workflow updated = new Workflow(
                workflow.id(), workflow.orgId(), workflow.createdBy(),
                workflow.name(), workflow.description(), workflow.steps(),
                workflow.trigger(), workflow.status(), workflow.metadata(),
                workflow.createdAt(), Instant.now()
        );
        workflowRepository.save(updated);
        return updated;
    }

    @Override
    public Workflow activate(UUID workflowId) {
        Workflow w = getById(workflowId);
        Workflow activated = new Workflow(
                w.id(), w.orgId(), w.createdBy(), w.name(), w.description(),
                w.steps(), w.trigger(), Workflow.WorkflowStatus.ACTIVE,
                w.metadata(), w.createdAt(), Instant.now()
        );
        workflowRepository.save(activated);
        log.info("Workflow activated: id={}", workflowId);
        return activated;
    }

    @Override
    public Workflow pause(UUID workflowId) {
        Workflow w = getById(workflowId);
        Workflow paused = new Workflow(
                w.id(), w.orgId(), w.createdBy(), w.name(), w.description(),
                w.steps(), w.trigger(), Workflow.WorkflowStatus.PAUSED,
                w.metadata(), w.createdAt(), Instant.now()
        );
        workflowRepository.save(paused);
        log.info("Workflow paused: id={}", workflowId);
        return paused;
    }

    @Override
    public void delete(UUID workflowId) {
        workflowRepository.delete(workflowId);
        log.info("Workflow deleted: id={}", workflowId);
    }

    @Override
    public WorkflowRun trigger(UUID workflowId, UUID triggeredBy) {
        Workflow workflow = getById(workflowId);
        WorkflowRun run = workflowRuntime.submit(workflow, triggeredBy);
        workflowRepository.saveRun(run);
        log.info("Workflow triggered: workflowId={}, runId={}", workflowId, run.id());
        return run;
    }

    @Override
    public WorkflowRun getRunById(UUID runId) {
        return workflowRepository.findRunById(runId)
                .orElseThrow(() -> new NoSuchElementException("Run not found: " + runId));
    }

    @Override
    public List<WorkflowRun> listRuns(UUID workflowId, int page, int size) {
        return workflowRepository.findRunsByWorkflow(workflowId, page * size, size);
    }

    @Override
    public WorkflowRun cancelRun(UUID runId) {
        workflowRuntime.cancel(runId);
        WorkflowRun current = workflowRuntime.queryState(runId);
        workflowRepository.saveRun(current);
        return current;
    }

    @Override
    public WorkflowRun approveStep(UUID runId, String stepId, UUID approverId) {
        workflowRuntime.signal(runId, "approve:" + stepId, approverId.toString());
        WorkflowRun current = workflowRuntime.queryState(runId);
        workflowRepository.saveRun(current);
        return current;
    }

    @Override
    public WorkflowRun rejectStep(UUID runId, String stepId, UUID approverId, String reason) {
        workflowRuntime.signal(runId, "reject:" + stepId,
                approverId.toString() + "|" + reason);
        WorkflowRun current = workflowRuntime.queryState(runId);
        workflowRepository.saveRun(current);
        return current;
    }
}
