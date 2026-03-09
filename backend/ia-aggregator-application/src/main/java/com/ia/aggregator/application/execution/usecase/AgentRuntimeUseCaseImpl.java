package com.ia.aggregator.application.execution.usecase;

import com.ia.aggregator.application.execution.port.in.AgentRuntimeUseCase;
import com.ia.aggregator.application.execution.port.out.AgentExecutionRepository;
import com.ia.aggregator.domain.execution.AgentExecution;
import com.ia.aggregator.domain.execution.AutonomyLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AgentRuntimeUseCaseImpl implements AgentRuntimeUseCase {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntimeUseCaseImpl.class);

    private final AgentExecutionRepository executionRepository;

    public AgentRuntimeUseCaseImpl(AgentExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Override
    public AgentExecution execute(UUID agentDefinitionId, UUID orgId, UUID userId,
                                   String objective, AutonomyLevel autonomyLevel,
                                   double maxBudgetUsd) {
        AgentExecution execution = new AgentExecution(
                UUID.randomUUID(), agentDefinitionId, orgId, userId,
                objective, autonomyLevel, AgentExecution.AgentStatus.PLANNING,
                new ArrayList<>(), 0.0, maxBudgetUsd,
                null, null, Instant.now(), null
        );
        executionRepository.save(execution);
        log.info("Agent execution started: id={}, agent={}, autonomy={}, objective={}",
                execution.id(), agentDefinitionId, autonomyLevel, objective);

        // In a full implementation, this would start the agentic loop:
        // 1. Plan → 2. Select Tool → 3. Execute → 4. Evaluate → 5. Next Step
        // The loop continues until objective is met, budget exceeded, or canceled.
        return execution;
    }

    @Override
    public AgentExecution getExecution(UUID executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new NoSuchElementException("Execution not found: " + executionId));
    }

    @Override
    public List<AgentExecution> listExecutions(UUID orgId, int page, int size) {
        return executionRepository.findByOrg(orgId, page * size, size);
    }

    @Override
    public AgentExecution approveAction(UUID executionId, int stepNumber) {
        AgentExecution execution = getExecution(executionId);
        log.info("Agent action approved: executionId={}, step={}", executionId, stepNumber);
        // In full implementation, signals the agent loop to continue
        return execution;
    }

    @Override
    public AgentExecution rejectAction(UUID executionId, int stepNumber, String reason) {
        AgentExecution execution = getExecution(executionId);
        log.info("Agent action rejected: executionId={}, step={}, reason={}",
                executionId, stepNumber, reason);
        return execution;
    }

    @Override
    public AgentExecution cancel(UUID executionId) {
        AgentExecution execution = getExecution(executionId);
        AgentExecution canceled = new AgentExecution(
                execution.id(), execution.agentDefinitionId(), execution.orgId(),
                execution.userId(), execution.objective(), execution.autonomyLevel(),
                AgentExecution.AgentStatus.CANCELED, execution.steps(),
                execution.totalCostUsd(), execution.maxBudgetUsd(),
                null, "Canceled by user", execution.startedAt(), Instant.now()
        );
        executionRepository.save(canceled);
        log.info("Agent execution canceled: id={}", executionId);
        return canceled;
    }

    @Override
    public AgentExecution resume(UUID executionId) {
        AgentExecution execution = getExecution(executionId);
        log.info("Agent execution resumed: id={}", executionId);
        return execution;
    }
}
