package com.ia.aggregator.application.billing.port.out;

import com.ia.aggregator.application.billing.port.in.BillingPlanUseCase.PlanDto;

import java.util.List;

/**
 * Port for billing plan queries.
 */
public interface BillingPlanRepository {

    List<PlanDto> findActivePlans();
}
