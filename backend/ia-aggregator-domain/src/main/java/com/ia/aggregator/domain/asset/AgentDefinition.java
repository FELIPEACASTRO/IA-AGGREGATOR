package com.ia.aggregator.domain.asset;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Definition of a custom AI agent with identity, tools, and constraints.
 */
public record AgentDefinition(
        UUID id,
        UUID orgId,
        String name,
        String description,
        String identity,
        String instructions,
        String defaultModel,
        UUID knowledgeBaseId,
        List<String> permittedTools,
        double maxBudgetUsd,
        boolean requiresApproval,
        AgentCategory category,
        Map<String, String> metadata
) {
    public enum AgentCategory {
        SALES, SUPPORT, LEGAL, MARKETING, ENGINEERING,
        FINANCE, HR, RESEARCH, OPS, CUSTOM
    }
}
