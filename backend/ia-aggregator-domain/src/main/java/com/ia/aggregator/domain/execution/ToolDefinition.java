package com.ia.aggregator.domain.execution;

import java.util.Map;
import java.util.UUID;

/**
 * A registered tool that agents can invoke.
 */
public record ToolDefinition(
        UUID id,
        String name,
        String description,
        String schemaJson,
        ToolScope scope,
        ToolRiskLevel riskLevel,
        double estimatedCostUsd,
        String endpoint,
        Map<String, String> headers,
        boolean requiresApproval,
        boolean enabled
) {
    public enum ToolScope {
        INTERNAL, EXTERNAL, MCP
    }

    public enum ToolRiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
