package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.ThreeDGenerationRequest;
import com.ia.aggregator.application.ai.dto.ThreeDGenerationResult;

/**
 * Use case port for 3D model generation.
 */
public interface ThreeDGenerationUseCase {

    ThreeDGenerationResult execute(ThreeDGenerationRequest request);
}
