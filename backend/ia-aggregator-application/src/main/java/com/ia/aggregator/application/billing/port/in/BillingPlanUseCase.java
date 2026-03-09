package com.ia.aggregator.application.billing.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Use case for querying billing plans.
 */
public interface BillingPlanUseCase {

    List<PlanDto> getActivePlans();

    record PlanDto(UUID id, String slug, String name, String tier, String priceDisplay,
                   int priceCents, String currency, String billingCycle, String description,
                   long tokenLimit, int modelCount, List<String> features, String gradient,
                   int sortOrder) {}
}
