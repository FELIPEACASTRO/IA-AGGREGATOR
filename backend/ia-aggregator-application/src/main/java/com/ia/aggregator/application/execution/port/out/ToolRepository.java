package com.ia.aggregator.application.execution.port.out;

import com.ia.aggregator.domain.execution.ToolDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for tool definition persistence.
 */
public interface ToolRepository {

    void save(ToolDefinition tool);

    Optional<ToolDefinition> findById(UUID id);

    List<ToolDefinition> findAll();

    List<ToolDefinition> findByScope(ToolDefinition.ToolScope scope);

    void delete(UUID id);
}
