package com.ia.aggregator.application.execution.port.in;

import com.ia.aggregator.domain.execution.AgentExecution;
import com.ia.aggregator.domain.execution.AutonomyLevel;

import java.util.List;
import java.util.UUID;

/**
 * Use case for agent runtime — agentic loop with tool use and reasoning.
 */
public interface AgentRuntimeUseCase {

    AgentExecution execute(UUID agentDefinitionId, UUID orgId, UUID userId,
                            String objective, AutonomyLevel autonomyLevel,
                            double maxBudgetUsd);

    AgentExecution getExecution(UUID executionId);

    List<AgentExecution> listExecutions(UUID orgId, int page, int size);

    AgentExecution approveAction(UUID executionId, int stepNumber);

    AgentExecution rejectAction(UUID executionId, int stepNumber, String reason);

    AgentExecution cancel(UUID executionId);

    AgentExecution resume(UUID executionId);
}
