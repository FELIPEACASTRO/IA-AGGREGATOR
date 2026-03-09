package com.ia.aggregator.application.ai.usecase;

import com.ia.aggregator.application.ai.port.in.ModelCatalogUseCase;
import com.ia.aggregator.application.ai.port.out.ModelCatalogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModelCatalogUseCaseImpl implements ModelCatalogUseCase {

    private final ModelCatalogRepository modelRepository;

    public ModelCatalogUseCaseImpl(ModelCatalogRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    @Override
    public List<ModelDto> getActiveModels() {
        return modelRepository.findActiveModels();
    }

    @Override
    public List<ModelDto> getDefaultModels() {
        return modelRepository.findDefaultModels();
    }
}
