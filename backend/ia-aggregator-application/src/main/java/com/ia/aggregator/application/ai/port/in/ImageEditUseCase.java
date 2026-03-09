package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.ImageEditRequest;
import com.ia.aggregator.application.ai.dto.ImageGenResult;

/**
 * Input port for image editing.
 * Modifies existing images based on text instructions.
 */
public interface ImageEditUseCase {
    ImageGenResult execute(ImageEditRequest request);
}
