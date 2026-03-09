package com.ia.aggregator.application.execution.port.out;

import com.ia.aggregator.domain.execution.AgentExecution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for agent execution persistence.
 */
public interface AgentExecutionRepository {

    void save(AgentExecution execution);

    Optional<AgentExecution> findById(UUID id);

    List<AgentExecution> findByOrg(UUID orgId, int offset, int limit);

    void delete(UUID id);
}
