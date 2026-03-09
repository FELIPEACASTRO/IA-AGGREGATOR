package com.ia.aggregator.application.ai.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Use case for querying the AI model catalog from the database.
 */
public interface ModelCatalogUseCase {

    List<ModelDto> getActiveModels();

    List<ModelDto> getDefaultModels();

    record ModelDto(UUID id, String modelId, String displayName, String provider,
                    String category, Integer contextWindow, Integer maxOutputTokens,
                    Double inputCostPer1k, Double outputCostPer1k,
                    boolean supportsStreaming, boolean supportsFunctionCalling,
                    boolean supportsVision, boolean isDefault, String tier) {}
}
