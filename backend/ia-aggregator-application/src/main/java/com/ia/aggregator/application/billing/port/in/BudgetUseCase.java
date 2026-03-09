package com.ia.aggregator.application.billing.port.in;

import com.ia.aggregator.domain.billing.BudgetAlert;

import java.util.List;
import java.util.UUID;

/**
 * Use case for budget management and cost controls.
 */
public interface BudgetUseCase {

    BudgetAlert createBudget(UUID orgId, String name, double budgetUsd, double threshold,
                              BudgetAlert.BudgetAction action);

    List<BudgetAlert> getBudgets(UUID orgId);

    void updateBudget(UUID budgetId, double budgetUsd, double threshold,
                       BudgetAlert.BudgetAction action, boolean enabled);

    void deleteBudget(UUID budgetId);

    /**
     * Check if an org has exceeded any budget thresholds.
     * Called before each AI request.
     */
    BudgetCheckResult checkBudget(UUID orgId);

    record BudgetCheckResult(boolean permitted, String reason, BudgetAlert.BudgetAction action) {
        public static BudgetCheckResult allow() {
            return new BudgetCheckResult(true, null, null);
        }

        public static BudgetCheckResult block(String reason, BudgetAlert.BudgetAction action) {
            return new BudgetCheckResult(false, reason, action);
        }
    }
}
