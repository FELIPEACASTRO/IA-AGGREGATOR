package com.ia.aggregator.domain.billing;

import java.util.UUID;

/**
 * Budget alert configuration.
 *
 * @param id          Alert ID
 * @param orgId       Organization
 * @param name        Alert name
 * @param budgetUsd   Monthly budget limit in USD
 * @param threshold   Alert threshold (0.0-1.0, e.g., 0.8 = alert at 80%)
 * @param action      What to do when threshold is hit: ALERT, THROTTLE, BLOCK
 * @param enabled     Whether this alert is active
 */
public record BudgetAlert(
        UUID id,
        UUID orgId,
        String name,
        double budgetUsd,
        double threshold,
        BudgetAction action,
        boolean enabled
) {
    public enum BudgetAction {
        ALERT,
        THROTTLE,
        BLOCK
    }
}
