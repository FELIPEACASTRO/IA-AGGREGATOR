package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.ThreeDGenerationRequest;
import com.ia.aggregator.application.ai.dto.ThreeDGenerationResult;

/**
 * Capability interface for 3D model generation.
 * Providers implementing this can generate 3D models from text or images.
 */
public interface ThreeDGenerationCapable {

    /**
     * Generates a 3D model from text prompt or reference image.
     *
     * @param request the 3D generation request
     * @return the result with download URL and format
     */
    ThreeDGenerationResult generate3D(ThreeDGenerationRequest request);
}
