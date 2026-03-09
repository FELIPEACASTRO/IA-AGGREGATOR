package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.ImageGenRequest;
import com.ia.aggregator.application.ai.dto.ImageGenResult;

/**
 * Capability interface for text-to-image generation.
 * Providers implementing this can generate images from text prompts.
 */
public interface ImageGenerationCapable {

    /**
     * Generates an image from a text prompt.
     *
     * @param request the image generation request with prompt, size, and style parameters
     * @return the result containing image URL(s) or base64-encoded data
     */
    ImageGenResult generateImage(ImageGenRequest request);
}
