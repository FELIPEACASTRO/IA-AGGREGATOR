package com.ia.aggregator.application.asset.port.out;

import com.ia.aggregator.domain.asset.AgentDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for agent definition persistence.
 */
public interface AgentRepository {

    void save(AgentDefinition agent);

    Optional<AgentDefinition> findById(UUID id);

    List<AgentDefinition> findByOrg(UUID orgId);

    List<AgentDefinition> findByCategory(UUID orgId, AgentDefinition.AgentCategory category);

    List<AgentDefinition> findPrebuilt();

    void delete(UUID id);
}
