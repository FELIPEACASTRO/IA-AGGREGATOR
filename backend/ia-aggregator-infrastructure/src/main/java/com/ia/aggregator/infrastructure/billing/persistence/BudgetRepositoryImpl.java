package com.ia.aggregator.infrastructure.billing.persistence;

import com.ia.aggregator.application.billing.port.out.BudgetRepository;
import com.ia.aggregator.domain.billing.BudgetAlert;
import com.ia.aggregator.infrastructure.billing.persistence.entity.BudgetJpaEntity;
import com.ia.aggregator.infrastructure.billing.persistence.repository.BudgetJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BudgetRepositoryImpl implements BudgetRepository {

    private final BudgetJpaRepository jpa;

    public BudgetRepositoryImpl(BudgetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(BudgetAlert alert) {
        BudgetJpaEntity entity = new BudgetJpaEntity();
        entity.setId(alert.id());
        entity.setOrgId(alert.orgId());
        entity.setName(alert.name());
        entity.setMonthlyLimitCents((int) (alert.budgetUsd() * 100));
        entity.setAlertThresholdPct((int) (alert.threshold() * 100));
        entity.setAction(alert.action().name());
        entity.setActive(alert.enabled());
        entity.setCurrentSpendCents(0);
        jpa.save(entity);
    }

    @Override
    public List<BudgetAlert> findByOrg(UUID orgId) {
        return jpa.findByOrgId(orgId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<BudgetAlert> findById(UUID budgetId) {
        return jpa.findById(budgetId).map(this::toDomain);
    }

    @Override
    public void update(BudgetAlert alert) {
        jpa.findById(alert.id()).ifPresent(entity -> {
            entity.setMonthlyLimitCents((int) (alert.budgetUsd() * 100));
            entity.setAlertThresholdPct((int) (alert.threshold() * 100));
            entity.setAction(alert.action().name());
            entity.setActive(alert.enabled());
            jpa.save(entity);
        });
    }

    @Override
    public void delete(UUID budgetId) {
        jpa.deleteById(budgetId);
    }

    private BudgetAlert toDomain(BudgetJpaEntity entity) {
        return new BudgetAlert(
                entity.getId(),
                entity.getOrgId(),
                entity.getName(),
                entity.getMonthlyLimitCents() / 100.0,
                entity.getAlertThresholdPct() / 100.0,
                BudgetAlert.BudgetAction.valueOf(entity.getAction()),
                entity.isActive()
        );
    }
}
