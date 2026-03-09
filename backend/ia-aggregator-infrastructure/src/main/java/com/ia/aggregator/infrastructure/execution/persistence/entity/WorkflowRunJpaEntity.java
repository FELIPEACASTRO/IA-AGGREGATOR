package com.ia.aggregator.infrastructure.execution.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_runs", schema = "execution")
public class WorkflowRunJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "triggered_by", nullable = false)
    private UUID triggeredBy;

    @Column(nullable = false)
    private String status;

    @Column(name = "current_step_id")
    private String currentStepId;

    @Column(name = "step_history", columnDefinition = "jsonb")
    private String stepHistory;

    @Column(columnDefinition = "jsonb")
    private String context;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public WorkflowRunJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public UUID getOrgId() { return orgId; }
    public UUID getTriggeredBy() { return triggeredBy; }
    public String getStatus() { return status; }
    public String getCurrentStepId() { return currentStepId; }
    public String getStepHistory() { return stepHistory; }
    public String getContext() { return context; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setTriggeredBy(UUID triggeredBy) { this.triggeredBy = triggeredBy; }
    public void setStatus(String status) { this.status = status; }
    public void setCurrentStepId(String currentStepId) { this.currentStepId = currentStepId; }
    public void setStepHistory(String stepHistory) { this.stepHistory = stepHistory; }
    public void setContext(String context) { this.context = context; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
