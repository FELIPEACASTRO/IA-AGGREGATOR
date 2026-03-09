package com.ia.aggregator.infrastructure.ai.persistence;

import com.ia.aggregator.application.ai.port.in.ModelCatalogUseCase.ModelDto;
import com.ia.aggregator.application.ai.port.out.ModelCatalogRepository;
import com.ia.aggregator.infrastructure.ai.persistence.entity.AiModelJpaEntity;
import com.ia.aggregator.infrastructure.ai.persistence.entity.AiProviderJpaEntity;
import com.ia.aggregator.infrastructure.ai.persistence.repository.AiModelJpaRepository;
import com.ia.aggregator.infrastructure.ai.persistence.repository.AiProviderJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ModelCatalogRepositoryImpl implements ModelCatalogRepository {

    private final AiModelJpaRepository modelJpa;
    private final AiProviderJpaRepository providerJpa;

    public ModelCatalogRepositoryImpl(AiModelJpaRepository modelJpa,
                                       AiProviderJpaRepository providerJpa) {
        this.modelJpa = modelJpa;
        this.providerJpa = providerJpa;
    }

    @Override
    public List<ModelDto> findActiveModels() {
        Map<UUID, String> providerNames = loadProviderNames();
        return modelJpa.findByIsActiveTrueOrderByProviderIdAscModelIdAsc().stream()
                .map(entity -> toDto(entity, providerNames))
                .toList();
    }

    @Override
    public List<ModelDto> findDefaultModels() {
        Map<UUID, String> providerNames = loadProviderNames();
        return modelJpa.findByIsDefaultTrue().stream()
                .map(entity -> toDto(entity, providerNames))
                .toList();
    }

    private ModelDto toDto(AiModelJpaEntity entity, Map<UUID, String> providerNames) {
        return new ModelDto(
                entity.getId(), entity.getModelId(),
                entity.getDisplayName() != null ? entity.getDisplayName() : entity.getModelId(),
                providerNames.getOrDefault(entity.getProviderId(), "unknown"),
                entity.getCategory(), entity.getContextWindow(), entity.getMaxOutputTokens(),
                entity.getInputCostPer1k() != null ? entity.getInputCostPer1k().doubleValue() : null,
                entity.getOutputCostPer1k() != null ? entity.getOutputCostPer1k().doubleValue() : null,
                entity.isSupportsStreaming(), entity.isSupportsFunctionCalling(),
                entity.isSupportsVision(), entity.isDefault(), entity.getTier()
        );
    }

    private Map<UUID, String> loadProviderNames() {
        return providerJpa.findAll().stream()
                .collect(Collectors.toMap(AiProviderJpaEntity::getId, AiProviderJpaEntity::getName,
                        (a, b) -> a));
    }
}
