package com.ia.aggregator.application.billing.usecase;

import com.ia.aggregator.application.billing.port.in.BudgetUseCase;
import com.ia.aggregator.application.billing.port.out.BudgetRepository;
import com.ia.aggregator.application.billing.port.out.UsageRepository;
import com.ia.aggregator.domain.billing.BudgetAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetUseCaseImpl implements BudgetUseCase {

    private static final Logger log = LoggerFactory.getLogger(BudgetUseCaseImpl.class);

    private final UsageRepository usageRepository;
    private final BudgetRepository budgetRepository;

    public BudgetUseCaseImpl(UsageRepository usageRepository, BudgetRepository budgetRepository) {
        this.usageRepository = usageRepository;
        this.budgetRepository = budgetRepository;
    }

    @Override
    public BudgetAlert createBudget(UUID orgId, String name, double budgetUsd, double threshold,
                                     BudgetAlert.BudgetAction action) {
        BudgetAlert alert = new BudgetAlert(UUID.randomUUID(), orgId, name, budgetUsd, threshold, action, true);
        budgetRepository.save(alert);
        log.info("Budget created: org={}, name={}, budget=${}, threshold={}%, action={}",
                orgId, name, budgetUsd, threshold * 100, action);
        return alert;
    }

    @Override
    public List<BudgetAlert> getBudgets(UUID orgId) {
        return budgetRepository.findByOrg(orgId);
    }

    @Override
    public void updateBudget(UUID budgetId, double budgetUsd, double threshold,
                              BudgetAlert.BudgetAction action, boolean enabled) {
        budgetRepository.findById(budgetId).ifPresent(existing -> {
            BudgetAlert updated = new BudgetAlert(
                    existing.id(), existing.orgId(), existing.name(),
                    budgetUsd, threshold, action, enabled
            );
            budgetRepository.update(updated);
            log.info("Budget updated: id={}, budget=${}, threshold={}%", budgetId, budgetUsd, threshold * 100);
        });
    }

    @Override
    public void deleteBudget(UUID budgetId) {
        budgetRepository.delete(budgetId);
        log.info("Budget deleted: id={}", budgetId);
    }

    @Override
    public BudgetCheckResult checkBudget(UUID orgId) {
        List<BudgetAlert> orgBudgets = getBudgets(orgId);
        if (orgBudgets.isEmpty()) {
            return BudgetCheckResult.allow();
        }

        Instant monthStart = Instant.now().minus(30, ChronoUnit.DAYS);
        double currentSpend = usageRepository.sumCostByOrg(orgId, monthStart, Instant.now());

        for (BudgetAlert alert : orgBudgets) {
            if (!alert.enabled()) continue;

            double thresholdAmount = alert.budgetUsd() * alert.threshold();
            if (currentSpend >= thresholdAmount) {
                log.warn("Budget threshold exceeded: org={}, budget={}, spend=${}, threshold=${}",
                        orgId, alert.name(), currentSpend, thresholdAmount);

                if (alert.action() == BudgetAlert.BudgetAction.BLOCK && currentSpend >= alert.budgetUsd()) {
                    return BudgetCheckResult.block(
                            "Monthly budget of $" + alert.budgetUsd() + " exceeded (current: $" +
                                    String.format("%.2f", currentSpend) + ")",
                            alert.action()
                    );
                }

                if (alert.action() == BudgetAlert.BudgetAction.THROTTLE && currentSpend >= alert.budgetUsd()) {
                    return BudgetCheckResult.block(
                            "Monthly budget of $" + alert.budgetUsd() + " exceeded — requests throttled",
                            alert.action()
                    );
                }
            }
        }

        return BudgetCheckResult.allow();
    }
}
