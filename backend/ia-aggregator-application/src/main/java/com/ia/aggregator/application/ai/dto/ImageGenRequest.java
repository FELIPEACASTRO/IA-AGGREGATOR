package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for image generation.
 *
 * @param prompt text description of the image to generate (required)
 * @param model preferred image generation model (optional)
 * @param width desired image width in pixels (optional)
 * @param height desired image height in pixels (optional)
 * @param style style preset (e.g., "natural", "vivid", "anime") (optional)
 * @param numberOfImages number of images to generate (optional, default 1)
 * @param responseFormat "url" or "b64_json" (optional, default "url")
 */
public record ImageGenRequest(
        @NotBlank(message = "prompt is required")
        String prompt,
        String model,
        Integer width,
        Integer height,
        String style,
        Integer numberOfImages,
        String responseFormat
) {
    /** Convenience constructor for simple prompt-only generation. */
    public ImageGenRequest(String prompt, String model) {
        this(prompt, model, null, null, null, null, null);
    }
}
