package com.ia.aggregator.infrastructure.billing.persistence;

import com.ia.aggregator.application.billing.port.in.SubscriptionUseCase.SubscriptionInfo;
import com.ia.aggregator.application.billing.port.out.SubscriptionRepository;
import com.ia.aggregator.domain.billing.PlanTier;
import com.ia.aggregator.domain.billing.SubscriptionStatus;
import com.ia.aggregator.infrastructure.billing.persistence.entity.SubscriptionJpaEntity;
import com.ia.aggregator.infrastructure.billing.persistence.repository.BillingPlanJpaRepository;
import com.ia.aggregator.infrastructure.billing.persistence.repository.SubscriptionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final SubscriptionJpaRepository subscriptionJpa;
    private final BillingPlanJpaRepository planJpa;

    public SubscriptionRepositoryImpl(SubscriptionJpaRepository subscriptionJpa,
                                       BillingPlanJpaRepository planJpa) {
        this.subscriptionJpa = subscriptionJpa;
        this.planJpa = planJpa;
    }

    @Override
    public void save(SubscriptionInfo info) {
        SubscriptionJpaEntity entity = subscriptionJpa
                .findByOrgIdAndStatus(info.orgId(), "active")
                .orElseGet(SubscriptionJpaEntity::new);

        entity.setId(entity.getId() != null ? entity.getId() : UUID.randomUUID());
        entity.setOrgId(info.orgId());
        entity.setUserId(info.orgId()); // org-level subscription
        entity.setStatus(info.status().name().toLowerCase());
        entity.setCurrentPeriodStart(info.currentPeriodStart());
        entity.setCurrentPeriodEnd(info.currentPeriodEnd());
        entity.setStripeSubscriptionId(info.stripeSubscriptionId());

        // Resolve plan ID from tier
        PlanTier tier = info.tier();
        String tierStr = tier.name().toLowerCase();
        planJpa.findByTier(tierStr).ifPresent(plan -> entity.setPlanId(plan.getId()));

        subscriptionJpa.save(entity);
    }

    @Override
    public Optional<SubscriptionInfo> findActiveByOrg(UUID orgId) {
        return subscriptionJpa.findByOrgIdAndStatus(orgId, "active")
                .map(this::toDomain);
    }

    @Override
    public void updateStatus(UUID orgId, String status) {
        subscriptionJpa.findByOrgIdAndStatus(orgId, "active").ifPresent(entity -> {
            entity.setStatus(status);
            subscriptionJpa.save(entity);
        });
    }

    private SubscriptionInfo toDomain(SubscriptionJpaEntity entity) {
        PlanTier tier = resolveTier(entity.getPlanId());
        SubscriptionStatus status = SubscriptionStatus.valueOf(entity.getStatus().toUpperCase());
        return new SubscriptionInfo(
                entity.getOrgId(), tier, status,
                entity.getStripeSubscriptionId(), null,
                entity.getCurrentPeriodStart(), entity.getCurrentPeriodEnd()
        );
    }

    private PlanTier resolveTier(UUID planId) {
        if (planId == null) return PlanTier.FREE;
        return planJpa.findById(planId)
                .map(plan -> {
                    try {
                        return PlanTier.valueOf(plan.getTier().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return PlanTier.FREE;
                    }
                })
                .orElse(PlanTier.FREE);
    }
}
