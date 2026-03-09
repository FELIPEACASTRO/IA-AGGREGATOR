package com.ia.aggregator.application.billing.port.out;

import com.ia.aggregator.domain.billing.UsageRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Port for usage record persistence.
 */
public interface UsageRepository {

    void save(UsageRecord record);

    List<UsageRecord> findByOrg(UUID orgId, Instant from, Instant to);

    double sumCostByOrg(UUID orgId, Instant from, Instant to);

    long countByOrg(UUID orgId, Instant from, Instant to);
}
