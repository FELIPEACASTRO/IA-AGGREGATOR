package com.ia.aggregator.application.billing.port.in;

import com.ia.aggregator.domain.billing.PlanTier;
import com.ia.aggregator.domain.billing.SubscriptionStatus;

import java.util.UUID;

/**
 * Use case for subscription management.
 */
public interface SubscriptionUseCase {

    SubscriptionInfo getSubscription(UUID orgId);

    SubscriptionInfo subscribe(UUID orgId, PlanTier tier, String paymentMethodId);

    SubscriptionInfo changePlan(UUID orgId, PlanTier newTier);

    void cancelSubscription(UUID orgId);

    record SubscriptionInfo(UUID orgId, PlanTier tier, SubscriptionStatus status,
                             String stripeSubscriptionId, String stripeCustomerId,
                             java.time.Instant currentPeriodStart,
                             java.time.Instant currentPeriodEnd) {}
}
