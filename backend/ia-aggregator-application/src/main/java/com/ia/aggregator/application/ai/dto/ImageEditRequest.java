package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for image editing.
 *
 * @param imageData source image as base64-encoded bytes or URL (required)
 * @param prompt edit instruction (required)
 * @param model preferred model (optional)
 * @param maskData optional mask image (base64) defining the area to edit
 * @param outputFormat "url" or "b64_json" (optional)
 */
public record ImageEditRequest(
        @NotNull(message = "image data is required")
        String imageData,
        @NotBlank(message = "prompt is required")
        String prompt,
        String model,
        String maskData,
        String outputFormat
) {
}
