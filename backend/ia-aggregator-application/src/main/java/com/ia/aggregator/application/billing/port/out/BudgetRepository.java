package com.ia.aggregator.application.billing.port.out;

import com.ia.aggregator.domain.billing.BudgetAlert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for budget alert persistence.
 */
public interface BudgetRepository {

    void save(BudgetAlert alert);

    List<BudgetAlert> findByOrg(UUID orgId);

    Optional<BudgetAlert> findById(UUID budgetId);

    void update(BudgetAlert alert);

    void delete(UUID budgetId);
}
