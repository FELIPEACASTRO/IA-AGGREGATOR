package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Result DTO for image generation and image editing.
 *
 * @param images list of generated image data
 * @param modelUsed actual model used
 * @param providerUsed provider that generated the images
 */
public record ImageGenResult(
        List<GeneratedImage> images,
        String modelUsed,
        String providerUsed
) {
    /**
     * A single generated image.
     *
     * @param url URL to the generated image (null if base64)
     * @param base64 base64-encoded image data (null if URL)
     * @param revisedPrompt the prompt as revised by the model (optional)
     */
    public record GeneratedImage(String url, String base64, String revisedPrompt) {
    }
}
