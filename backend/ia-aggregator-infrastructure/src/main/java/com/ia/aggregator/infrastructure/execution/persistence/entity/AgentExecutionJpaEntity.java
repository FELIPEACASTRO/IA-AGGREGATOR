package com.ia.aggregator.infrastructure.execution.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_executions", schema = "execution")
public class AgentExecutionJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "agent_definition_id", nullable = false)
    private UUID agentDefinitionId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objective;

    @Column(name = "autonomy_level", nullable = false)
    private String autonomyLevel;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "jsonb")
    private String steps;

    @Column(name = "total_cost_usd", nullable = false)
    private BigDecimal totalCostUsd;

    @Column(name = "max_budget_usd", nullable = false)
    private BigDecimal maxBudgetUsd;

    @Column(name = "final_answer", columnDefinition = "TEXT")
    private String finalAnswer;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public AgentExecutionJpaEntity() {}

    // Getters
    public UUID getId() { return id; }
    public UUID getAgentDefinitionId() { return agentDefinitionId; }
    public UUID getOrgId() { return orgId; }
    public UUID getUserId() { return userId; }
    public String getObjective() { return objective; }
    public String getAutonomyLevel() { return autonomyLevel; }
    public String getStatus() { return status; }
    public String getSteps() { return steps; }
    public BigDecimal getTotalCostUsd() { return totalCostUsd; }
    public BigDecimal getMaxBudgetUsd() { return maxBudgetUsd; }
    public String getFinalAnswer() { return finalAnswer; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setAgentDefinitionId(UUID agentDefinitionId) { this.agentDefinitionId = agentDefinitionId; }
    public void setOrgId(UUID orgId) { this.orgId = orgId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setObjective(String objective) { this.objective = objective; }
    public void setAutonomyLevel(String autonomyLevel) { this.autonomyLevel = autonomyLevel; }
    public void setStatus(String status) { this.status = status; }
    public void setSteps(String steps) { this.steps = steps; }
    public void setTotalCostUsd(BigDecimal totalCostUsd) { this.totalCostUsd = totalCostUsd; }
    public void setMaxBudgetUsd(BigDecimal maxBudgetUsd) { this.maxBudgetUsd = maxBudgetUsd; }
    public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
