package com.ia.aggregator.application.asset.port.in;

import com.ia.aggregator.domain.asset.AgentDefinition;

import java.util.List;
import java.util.UUID;

/**
 * Use case for agent definition management.
 */
public interface AgentUseCase {

    AgentDefinition create(AgentDefinition agent);

    AgentDefinition getById(UUID agentId);

    List<AgentDefinition> listByOrg(UUID orgId);

    List<AgentDefinition> listByCategory(UUID orgId, AgentDefinition.AgentCategory category);

    List<AgentDefinition> listPrebuilt();

    AgentDefinition update(AgentDefinition agent);

    void delete(UUID agentId);
}
