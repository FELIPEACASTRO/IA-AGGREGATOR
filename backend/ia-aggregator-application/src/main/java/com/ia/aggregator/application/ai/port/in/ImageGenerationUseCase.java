package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.ImageGenRequest;
import com.ia.aggregator.application.ai.dto.ImageGenResult;

/**
 * Input port for image generation.
 * Creates images from text prompts.
 */
public interface ImageGenerationUseCase {
    ImageGenResult execute(ImageGenRequest request);
}
