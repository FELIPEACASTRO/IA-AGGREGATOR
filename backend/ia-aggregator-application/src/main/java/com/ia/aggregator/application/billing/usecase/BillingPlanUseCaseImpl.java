package com.ia.aggregator.application.billing.usecase;

import com.ia.aggregator.application.billing.port.in.BillingPlanUseCase;
import com.ia.aggregator.application.billing.port.out.BillingPlanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BillingPlanUseCaseImpl implements BillingPlanUseCase {

    private final BillingPlanRepository planRepository;

    public BillingPlanUseCaseImpl(BillingPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public List<PlanDto> getActivePlans() {
        return planRepository.findActivePlans();
    }
}
