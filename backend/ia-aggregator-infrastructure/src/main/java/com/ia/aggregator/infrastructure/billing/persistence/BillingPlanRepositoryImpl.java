package com.ia.aggregator.infrastructure.billing.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.billing.port.in.BillingPlanUseCase.PlanDto;
import com.ia.aggregator.application.billing.port.out.BillingPlanRepository;
import com.ia.aggregator.infrastructure.billing.persistence.entity.BillingPlanJpaEntity;
import com.ia.aggregator.infrastructure.billing.persistence.repository.BillingPlanJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BillingPlanRepositoryImpl implements BillingPlanRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final BillingPlanJpaRepository jpa;

    public BillingPlanRepositoryImpl(BillingPlanJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<PlanDto> findActivePlans() {
        return jpa.findByIsActiveTrueOrderBySortOrder().stream()
                .map(this::toDto)
                .toList();
    }

    private PlanDto toDto(BillingPlanJpaEntity entity) {
        List<String> features;
        try {
            features = objectMapper.readValue(
                    entity.getFeatures() != null ? entity.getFeatures() : "[]", STRING_LIST);
        } catch (Exception e) {
            features = List.of();
        }

        return new PlanDto(
                entity.getId(), entity.getSlug(), entity.getName(), entity.getTier(),
                entity.getPriceDisplay(), entity.getPriceCents(), entity.getCurrency(),
                entity.getBillingCycle(), entity.getDescription(), entity.getTokenLimit(),
                entity.getModelCount(), features, entity.getGradient(), entity.getSortOrder()
        );
    }
}
