package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.application.ai.port.in.ModelCatalogUseCase.ModelDto;

import java.util.List;

/**
 * Port for querying AI models from database.
 */
public interface ModelCatalogRepository {

    List<ModelDto> findActiveModels();

    List<ModelDto> findDefaultModels();
}
