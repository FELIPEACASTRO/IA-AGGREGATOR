package com.ia.aggregator.application.billing.port.out;

import com.ia.aggregator.application.billing.port.in.SubscriptionUseCase.SubscriptionInfo;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for subscription persistence.
 */
public interface SubscriptionRepository {

    void save(SubscriptionInfo info);

    Optional<SubscriptionInfo> findActiveByOrg(UUID orgId);

    void updateStatus(UUID orgId, String status);
}
