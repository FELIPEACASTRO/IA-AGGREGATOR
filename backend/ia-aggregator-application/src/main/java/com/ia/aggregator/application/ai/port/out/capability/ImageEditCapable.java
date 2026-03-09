package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.ImageEditRequest;
import com.ia.aggregator.application.ai.dto.ImageGenResult;

/**
 * Capability interface for image editing with text instructions.
 * Providers implementing this can modify existing images based on prompts.
 */
public interface ImageEditCapable {

    /**
     * Edits an existing image based on text instructions.
     *
     * @param request the image edit request with source image, prompt, and mask
     * @return the result containing the edited image URL or base64 data
     */
    ImageGenResult editImage(ImageEditRequest request);
}
