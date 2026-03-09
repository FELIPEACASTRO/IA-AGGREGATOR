package com.ia.aggregator.infrastructure.gateway;

import com.ia.aggregator.application.gateway.port.out.RoutingRuleRepository;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.domain.ai.RoutingStrategy;
import com.ia.aggregator.domain.gateway.RoutingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * YAML-backed routing rule repository.
 *
 * <p>Rules are loaded from application.yml under {@code app.ai.routing.rules}.
 * Supports hot reload via {@link #reload()}.
 */
@Component
@ConfigurationProperties(prefix = "app.ai.routing")
public class YamlRoutingRuleRepository implements RoutingRuleRepository {

    private static final Logger log = LoggerFactory.getLogger(YamlRoutingRuleRepository.class);

    private List<RuleConfig> rules = new ArrayList<>();
    private final CopyOnWriteArrayList<RoutingRule> parsedRules = new CopyOnWriteArrayList<>();

    @Override
    public List<RoutingRule> findAllEnabled() {
        ensureParsed();
        return parsedRules.stream()
                .filter(RoutingRule::enabled)
                .sorted(Comparator.comparingInt(RoutingRule::priority))
                .collect(Collectors.toList());
    }

    @Override
    public List<RoutingRule> findByCapability(Capability capability) {
        return findAllEnabled().stream()
                .filter(r -> r.appliesTo(capability))
                .collect(Collectors.toList());
    }

    @Override
    public void reload() {
        parsedRules.clear();
        parseRules();
        log.info("Reloaded {} routing rules", parsedRules.size());
    }

    // Called by Spring configuration binding
    public void setRules(List<RuleConfig> rules) {
        this.rules = rules != null ? rules : new ArrayList<>();
        parsedRules.clear();
    }

    public List<RuleConfig> getRules() {
        return rules;
    }

    private void ensureParsed() {
        if (parsedRules.isEmpty() && !rules.isEmpty()) {
            parseRules();
        }
    }

    private void parseRules() {
        for (RuleConfig cfg : rules) {
            try {
                RoutingRule rule = new RoutingRule(
                        cfg.getName(),
                        cfg.getPriority(),
                        parseStrategy(cfg.getStrategy()),
                        parseCapabilities(cfg.getCapabilities()),
                        cfg.getAllowedModels() != null ? cfg.getAllowedModels() : List.of(),
                        cfg.getBlockedModels() != null ? cfg.getBlockedModels() : List.of(),
                        cfg.getProviderWeights() != null ? cfg.getProviderWeights() : Map.of(),
                        cfg.getMaxCostPerRequest(),
                        cfg.getMaxLatencyMs(),
                        cfg.getConditions() != null ? cfg.getConditions() : Map.of(),
                        cfg.isEnabled()
                );
                parsedRules.add(rule);
            } catch (Exception e) {
                log.warn("Failed to parse routing rule '{}': {}", cfg.getName(), e.getMessage());
            }
        }
    }

    private RoutingStrategy parseStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) return RoutingStrategy.BALANCED_DEFAULT;
        return RoutingStrategy.valueOf(strategy.toUpperCase().replace('-', '_'));
    }

    private List<Capability> parseCapabilities(List<String> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) return List.of();
        return capabilities.stream()
                .map(c -> Capability.valueOf(c.toUpperCase().replace('-', '_')))
                .collect(Collectors.toList());
    }

    /**
     * YAML config binding class for routing rules.
     */
    public static class RuleConfig {
        private String name = "";
        private int priority = 100;
        private String strategy = "balanced-default";
        private List<String> capabilities;
        private List<String> allowedModels;
        private List<String> blockedModels;
        private Map<String, Integer> providerWeights;
        private double maxCostPerRequest = 0;
        private long maxLatencyMs = 0;
        private Map<String, String> conditions;
        private boolean enabled = true;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public List<String> getCapabilities() { return capabilities; }
        public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
        public List<String> getAllowedModels() { return allowedModels; }
        public void setAllowedModels(List<String> allowedModels) { this.allowedModels = allowedModels; }
        public List<String> getBlockedModels() { return blockedModels; }
        public void setBlockedModels(List<String> blockedModels) { this.blockedModels = blockedModels; }
        public Map<String, Integer> getProviderWeights() { return providerWeights; }
        public void setProviderWeights(Map<String, Integer> providerWeights) { this.providerWeights = providerWeights; }
        public double getMaxCostPerRequest() { return maxCostPerRequest; }
        public void setMaxCostPerRequest(double maxCostPerRequest) { this.maxCostPerRequest = maxCostPerRequest; }
        public long getMaxLatencyMs() { return maxLatencyMs; }
        public void setMaxLatencyMs(long maxLatencyMs) { this.maxLatencyMs = maxLatencyMs; }
        public Map<String, String> getConditions() { return conditions; }
        public void setConditions(Map<String, String> conditions) { this.conditions = conditions; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
