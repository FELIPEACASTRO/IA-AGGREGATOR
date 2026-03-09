package com.ia.aggregator.application.gateway.port.out;

import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.domain.gateway.RoutingRule;

import java.util.List;

/**
 * Port for loading routing rules.
 */
public interface RoutingRuleRepository {

    /**
     * Load all enabled routing rules, sorted by priority.
     */
    List<RoutingRule> findAllEnabled();

    /**
     * Load rules applicable to a specific capability.
     */
    List<RoutingRule> findByCapability(Capability capability);

    /**
     * Reload rules from configuration source (YAML, database, etc.).
     */
    void reload();
}
