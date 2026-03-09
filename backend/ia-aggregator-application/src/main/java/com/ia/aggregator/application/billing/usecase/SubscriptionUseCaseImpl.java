package com.ia.aggregator.application.billing.usecase;

import com.ia.aggregator.application.billing.port.in.SubscriptionUseCase;
import com.ia.aggregator.application.billing.port.out.PaymentGatewayPort;
import com.ia.aggregator.application.billing.port.out.SubscriptionRepository;
import com.ia.aggregator.domain.billing.PlanTier;
import com.ia.aggregator.domain.billing.SubscriptionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class SubscriptionUseCaseImpl implements SubscriptionUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionUseCaseImpl.class);

    private final PaymentGatewayPort paymentGateway;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${app.billing.stripe.prices.starter:}")
    private String starterPriceId;
    @Value("${app.billing.stripe.prices.pro:}")
    private String proPriceId;
    @Value("${app.billing.stripe.prices.business:}")
    private String businessPriceId;
    @Value("${app.billing.stripe.prices.enterprise:}")
    private String enterprisePriceId;

    public SubscriptionUseCaseImpl(PaymentGatewayPort paymentGateway,
                                    SubscriptionRepository subscriptionRepository) {
        this.paymentGateway = paymentGateway;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public SubscriptionInfo getSubscription(UUID orgId) {
        return subscriptionRepository.findActiveByOrg(orgId)
                .orElse(new SubscriptionInfo(
                        orgId, PlanTier.FREE, SubscriptionStatus.ACTIVE,
                        null, null, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS)
                ));
    }

    @Override
    public SubscriptionInfo subscribe(UUID orgId, PlanTier tier, String paymentMethodId) {
        if (tier == PlanTier.FREE) {
            throw new IllegalArgumentException("Cannot subscribe to FREE tier via payment");
        }

        String priceId = resolvePriceId(tier);
        String customerId = paymentGateway.createCustomer(
                orgId + "@org.local", orgId.toString(),
                Map.of("orgId", orgId.toString(), "tier", tier.name())
        );
        String subscriptionId = paymentGateway.createSubscription(customerId, priceId);

        Instant now = Instant.now();
        SubscriptionInfo info = new SubscriptionInfo(
                orgId, tier, SubscriptionStatus.ACTIVE,
                subscriptionId, customerId, now, now.plus(30, ChronoUnit.DAYS)
        );
        subscriptionRepository.save(info);
        log.info("Subscription created: org={}, tier={}, stripeSubId={}", orgId, tier, subscriptionId);
        return info;
    }

    @Override
    public SubscriptionInfo changePlan(UUID orgId, PlanTier newTier) {
        SubscriptionInfo current = getSubscription(orgId);
        if (current.stripeSubscriptionId() == null) {
            throw new IllegalStateException("No active subscription to change for org: " + orgId);
        }

        String newPriceId = resolvePriceId(newTier);
        paymentGateway.changeSubscription(current.stripeSubscriptionId(), newPriceId);

        SubscriptionInfo updated = new SubscriptionInfo(
                orgId, newTier, SubscriptionStatus.ACTIVE,
                current.stripeSubscriptionId(), current.stripeCustomerId(),
                current.currentPeriodStart(), current.currentPeriodEnd()
        );
        subscriptionRepository.save(updated);
        log.info("Plan changed: org={}, newTier={}", orgId, newTier);
        return updated;
    }

    @Override
    public void cancelSubscription(UUID orgId) {
        SubscriptionInfo current = getSubscription(orgId);
        if (current.stripeSubscriptionId() == null) {
            throw new IllegalStateException("No active subscription to cancel for org: " + orgId);
        }

        paymentGateway.cancelSubscription(current.stripeSubscriptionId());

        SubscriptionInfo canceled = new SubscriptionInfo(
                orgId, current.tier(), SubscriptionStatus.CANCELED,
                current.stripeSubscriptionId(), current.stripeCustomerId(),
                current.currentPeriodStart(), current.currentPeriodEnd()
        );
        subscriptionRepository.save(canceled);
        log.info("Subscription canceled: org={}", orgId);
    }

    private String resolvePriceId(PlanTier tier) {
        return switch (tier) {
            case STARTER -> starterPriceId;
            case PRO -> proPriceId;
            case BUSINESS -> businessPriceId;
            case ENTERPRISE -> enterprisePriceId;
            case FREE -> throw new IllegalArgumentException("FREE tier has no price ID");
        };
    }
}
