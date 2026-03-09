package com.ia.aggregator.application.gateway.usecase;

import com.ia.aggregator.application.gateway.port.in.RoutingUseCase;
import com.ia.aggregator.application.gateway.port.out.ProviderMetricsPort;
import com.ia.aggregator.application.gateway.port.out.RoutingRuleRepository;
import com.ia.aggregator.domain.ai.RoutingStrategy;
import com.ia.aggregator.domain.gateway.RoutingContext;
import com.ia.aggregator.domain.gateway.RoutingDecision;
import com.ia.aggregator.domain.gateway.RoutingDecision.ProviderCandidate;
import com.ia.aggregator.domain.gateway.RoutingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Intelligent routing engine that evaluates rules and applies strategy-based provider selection.
 *
 * <p>Flow: rules → match → strategy → rank → traffic split → candidates.
 */
@Service
public class RoutingUseCaseImpl implements RoutingUseCase {

    private static final Logger log = LoggerFactory.getLogger(RoutingUseCaseImpl.class);

    private final RoutingRuleRepository ruleRepository;
    private final ProviderMetricsPort metricsPort;

    public RoutingUseCaseImpl(RoutingRuleRepository ruleRepository,
                               ProviderMetricsPort metricsPort) {
        this.ruleRepository = ruleRepository;
        this.metricsPort = metricsPort;
    }

    @Override
    public RoutingDecision route(RoutingContext context) {
        List<RoutingRule> rules = ruleRepository.findByCapability(context.capability());

        // Find first matching rule
        RoutingRule matchedRule = null;
        for (RoutingRule rule : rules) {
            if (matchesConditions(rule, context)) {
                matchedRule = rule;
                break;
            }
        }

        RoutingStrategy effectiveStrategy = resolveStrategy(context, matchedRule);
        List<ProviderCandidate> candidates;

        if (matchedRule != null && !matchedRule.providerWeights().isEmpty()) {
            // Use weighted providers from rule
            candidates = buildWeightedCandidates(matchedRule, effectiveStrategy);
        } else {
            // Build candidates from all available providers
            candidates = buildDefaultCandidates(context, effectiveStrategy, matchedRule);
        }

        // Apply traffic splitting if weights are specified
        if (matchedRule != null && !matchedRule.providerWeights().isEmpty()) {
            candidates = applyTrafficSplitting(candidates);
        }

        log.debug("Routing decision: strategy={}, rule={}, candidates={}",
                effectiveStrategy, matchedRule != null ? matchedRule.name() : "default", candidates.size());

        return new RoutingDecision(
                candidates,
                matchedRule != null ? matchedRule.name() : null,
                effectiveStrategy,
                false
        );
    }

    private boolean matchesConditions(RoutingRule rule, RoutingContext context) {
        Map<String, String> conditions = rule.conditions();
        if (conditions.isEmpty()) return true;

        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            String key = entry.getKey();
            String expected = entry.getValue();

            String actual = switch (key) {
                case "user-role" -> context.userRole();
                case "org-id" -> context.orgId() != null ? context.orgId().toString() : null;
                default -> context.metadata().get(key);
            };

            if (actual == null || !actual.equalsIgnoreCase(expected)) {
                return false;
            }
        }
        return true;
    }

    private RoutingStrategy resolveStrategy(RoutingContext context, RoutingRule rule) {
        if (context.strategy() != null) return context.strategy();
        if (rule != null) return rule.strategy();
        return RoutingStrategy.BALANCED_DEFAULT;
    }

    private List<ProviderCandidate> buildWeightedCandidates(RoutingRule rule, RoutingStrategy strategy) {
        List<ProviderCandidate> candidates = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : rule.providerWeights().entrySet()) {
            String providerName = entry.getKey();
            int weight = entry.getValue();

            // Use first allowed model for this provider
            String model = rule.allowedModels().isEmpty() ? "default" : rule.allowedModels().getFirst();

            double cost = metricsPort.getCostPer1kTokens(providerName, model);
            long latency = metricsPort.getP50LatencyMs(providerName, model);

            candidates.add(new ProviderCandidate(providerName, model, weight,
                    cost >= 0 ? cost : 0.01, latency >= 0 ? latency : 1000));
        }

        return rankByStrategy(candidates, strategy);
    }

    private List<ProviderCandidate> buildDefaultCandidates(
            RoutingContext context, RoutingStrategy strategy, RoutingRule rule) {
        List<ProviderCandidate> candidates = new ArrayList<>();

        // If user preferred a model, add it first
        if (context.preferredModel() != null && !context.preferredModel().isBlank()) {
            if (rule == null || rule.isModelAllowed(context.preferredModel())) {
                candidates.add(new ProviderCandidate(
                        "preferred", context.preferredModel(), 100,
                        metricsPort.getCostPer1kTokens("preferred", context.preferredModel()),
                        metricsPort.getP50LatencyMs("preferred", context.preferredModel())
                ));
            }
        }

        return candidates.isEmpty()
                ? List.of(new ProviderCandidate("default", "default", 100, 0.01, 1000))
                : candidates;
    }

    private List<ProviderCandidate> rankByStrategy(List<ProviderCandidate> candidates, RoutingStrategy strategy) {
        return switch (strategy) {
            case COST_OPTIMIZED -> candidates.stream()
                    .sorted(Comparator.comparingDouble(ProviderCandidate::estimatedCostUsd))
                    .collect(Collectors.toList());

            case LATENCY_OPTIMIZED -> candidates.stream()
                    .sorted(Comparator.comparingLong(ProviderCandidate::estimatedLatencyMs))
                    .collect(Collectors.toList());

            case QUALITY_OPTIMIZED -> candidates.stream()
                    .sorted((a, b) -> Double.compare(
                            metricsPort.getQualityScore(b.model()),
                            metricsPort.getQualityScore(a.model())))
                    .collect(Collectors.toList());

            case POLICY_STRICT -> candidates; // Already filtered by rule

            case BALANCED_DEFAULT -> candidates.stream()
                    .sorted(Comparator.comparingDouble(c -> {
                        double costScore = c.estimatedCostUsd() * 0.3;
                        double latencyScore = (c.estimatedLatencyMs() / 10000.0) * 0.3;
                        double qualityScore = (1 - metricsPort.getQualityScore(c.model())) * 0.4;
                        return costScore + latencyScore + qualityScore;
                    }))
                    .collect(Collectors.toList());
        };
    }

    /**
     * Apply weighted traffic splitting — select candidate based on weight distribution.
     */
    private List<ProviderCandidate> applyTrafficSplitting(List<ProviderCandidate> candidates) {
        if (candidates.size() <= 1) return candidates;

        int totalWeight = candidates.stream().mapToInt(ProviderCandidate::weight).sum();
        if (totalWeight <= 0) return candidates;

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        List<ProviderCandidate> reordered = new ArrayList<>(candidates);
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += candidates.get(i).weight();
            if (roll < cumulative) {
                // Move selected to front
                ProviderCandidate selected = reordered.remove(i);
                reordered.addFirst(selected);
                break;
            }
        }

        return reordered;
    }
}
