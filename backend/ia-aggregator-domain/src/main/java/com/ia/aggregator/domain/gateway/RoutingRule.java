package com.ia.aggregator.domain.gateway;

import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.domain.ai.RoutingStrategy;

import java.util.List;
import java.util.Map;

/**
 * A routing rule that maps request criteria to provider selection.
 *
 * <p>Rules are evaluated in priority order; the first matching rule determines
 * which providers and models to use.
 *
 * @param name           Human-readable rule name
 * @param priority       Lower number = higher priority (evaluated first)
 * @param strategy       The routing strategy to apply
 * @param capabilities   Which capabilities this rule applies to (empty = all)
 * @param allowedModels  Whitelist of allowed models (empty = all)
 * @param blockedModels  Blacklist of blocked models
 * @param providerWeights Weighted distribution for canary/traffic splitting (provider → weight 0-100)
 * @param maxCostPerRequest Maximum cost per request in USD (0 = unlimited)
 * @param maxLatencyMs   Maximum acceptable latency in ms (0 = unlimited)
 * @param conditions     Additional match conditions (header, user-role, org, etc.)
 * @param enabled        Whether this rule is active
 */
public record RoutingRule(
        String name,
        int priority,
        RoutingStrategy strategy,
        List<Capability> capabilities,
        List<String> allowedModels,
        List<String> blockedModels,
        Map<String, Integer> providerWeights,
        double maxCostPerRequest,
        long maxLatencyMs,
        Map<String, String> conditions,
        boolean enabled
) {
    public RoutingRule {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Rule name cannot be blank");
        }
        if (capabilities == null) capabilities = List.of();
        if (allowedModels == null) allowedModels = List.of();
        if (blockedModels == null) blockedModels = List.of();
        if (providerWeights == null) providerWeights = Map.of();
        if (conditions == null) conditions = Map.of();
    }

    /**
     * Check if this rule applies to the given capability.
     */
    public boolean appliesTo(Capability capability) {
        return capabilities.isEmpty() || capabilities.contains(capability);
    }

    /**
     * Check if a model is allowed by this rule.
     */
    public boolean isModelAllowed(String model) {
        if (!blockedModels.isEmpty() && blockedModels.contains(model)) {
            return false;
        }
        return allowedModels.isEmpty() || allowedModels.contains(model);
    }
}
