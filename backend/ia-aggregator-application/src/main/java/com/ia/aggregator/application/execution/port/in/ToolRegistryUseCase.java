package com.ia.aggregator.application.execution.port.in;

import com.ia.aggregator.domain.execution.ToolDefinition;

import java.util.List;
import java.util.UUID;

/**
 * Use case for tool registry management.
 */
public interface ToolRegistryUseCase {

    ToolDefinition register(ToolDefinition tool);

    ToolDefinition getById(UUID toolId);

    List<ToolDefinition> listAll();

    List<ToolDefinition> listByScope(ToolDefinition.ToolScope scope);

    ToolDefinition update(ToolDefinition tool);

    void disable(UUID toolId);

    void enable(UUID toolId);

    void delete(UUID toolId);

    ToolInvocationResult invoke(UUID toolId, String inputJson);

    record ToolInvocationResult(UUID toolId, String outputJson, long durationMs,
                                 double costUsd, boolean success, String error) {}
}
