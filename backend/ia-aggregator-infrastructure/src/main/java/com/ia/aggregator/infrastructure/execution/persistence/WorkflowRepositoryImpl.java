package com.ia.aggregator.infrastructure.execution.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ia.aggregator.application.execution.port.out.WorkflowRepository;
import com.ia.aggregator.domain.execution.Workflow;
import com.ia.aggregator.domain.execution.WorkflowRun;
import com.ia.aggregator.domain.execution.WorkflowStep;
import com.ia.aggregator.infrastructure.execution.persistence.entity.WorkflowJpaEntity;
import com.ia.aggregator.infrastructure.execution.persistence.entity.WorkflowRunJpaEntity;
import com.ia.aggregator.infrastructure.execution.persistence.repository.WorkflowJpaRepository;
import com.ia.aggregator.infrastructure.execution.persistence.repository.WorkflowRunJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WorkflowRepositoryImpl implements WorkflowRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final TypeReference<List<WorkflowStep>> STEPS_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, String>> MAP_STRING_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<WorkflowRun.StepExecution>> STEP_EXEC_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_OBJECT_TYPE = new TypeReference<>() {};

    private final WorkflowJpaRepository workflowJpa;
    private final WorkflowRunJpaRepository runJpa;

    public WorkflowRepositoryImpl(WorkflowJpaRepository workflowJpa, WorkflowRunJpaRepository runJpa) {
        this.workflowJpa = workflowJpa;
        this.runJpa = runJpa;
    }

    @Override
    public void save(Workflow workflow) {
        WorkflowJpaEntity entity = workflowJpa.findById(workflow.id()).orElseGet(WorkflowJpaEntity::new);
        entity.setId(workflow.id());
        entity.setOrgId(workflow.orgId());
        entity.setCreatedBy(workflow.createdBy());
        entity.setName(workflow.name());
        entity.setDescription(workflow.description());
        entity.setSteps(toJson(workflow.steps()));
        entity.setTriggerConfig(toJson(workflow.trigger()));
        entity.setStatus(workflow.status().name());
        entity.setMetadata(toJson(workflow.metadata()));
        workflowJpa.save(entity);
    }

    @Override
    public Optional<Workflow> findById(UUID id) {
        return workflowJpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Workflow> findByOrg(UUID orgId) {
        return workflowJpa.findByOrgId(orgId).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        workflowJpa.deleteById(id);
    }

    @Override
    public void saveRun(WorkflowRun run) {
        WorkflowRunJpaEntity entity = runJpa.findById(run.id()).orElseGet(WorkflowRunJpaEntity::new);
        entity.setId(run.id());
        entity.setWorkflowId(run.workflowId());
        entity.setOrgId(run.orgId());
        entity.setTriggeredBy(run.triggeredBy());
        entity.setStatus(run.status().name());
        entity.setCurrentStepId(run.currentStepId());
        entity.setStepHistory(toJson(run.stepHistory()));
        entity.setContext(toJson(run.context()));
        entity.setErrorMessage(run.errorMessage());
        entity.setStartedAt(run.startedAt());
        entity.setCompletedAt(run.completedAt());
        runJpa.save(entity);
    }

    @Override
    public Optional<WorkflowRun> findRunById(UUID id) {
        return runJpa.findById(id).map(this::runToDomain);
    }

    @Override
    public List<WorkflowRun> findRunsByWorkflow(UUID workflowId, int offset, int limit) {
        int safeLimit = Math.max(1, limit);
        return runJpa.findByWorkflowId(workflowId, PageRequest.of(offset / safeLimit, safeLimit))
                .stream().map(this::runToDomain).toList();
    }

    private Workflow toDomain(WorkflowJpaEntity entity) {
        return new Workflow(
                entity.getId(),
                entity.getOrgId(),
                entity.getCreatedBy(),
                entity.getName(),
                entity.getDescription(),
                fromJson(entity.getSteps(), STEPS_TYPE, List.of()),
                fromJson(entity.getTriggerConfig(), new TypeReference<Workflow.TriggerConfig>() {}, null),
                Workflow.WorkflowStatus.valueOf(entity.getStatus()),
                fromJson(entity.getMetadata(), MAP_STRING_TYPE, Map.of()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private WorkflowRun runToDomain(WorkflowRunJpaEntity entity) {
        return new WorkflowRun(
                entity.getId(),
                entity.getWorkflowId(),
                entity.getOrgId(),
                entity.getTriggeredBy(),
                WorkflowRun.RunStatus.valueOf(entity.getStatus()),
                entity.getCurrentStepId(),
                fromJson(entity.getStepHistory(), STEP_EXEC_TYPE, List.of()),
                fromJson(entity.getContext(), MAP_OBJECT_TYPE, Map.of()),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj != null ? obj : Map.of());
        } catch (Exception e) {
            return "{}";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type, T defaultValue) {
        try {
            if (json == null || json.isBlank()) return defaultValue;
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
