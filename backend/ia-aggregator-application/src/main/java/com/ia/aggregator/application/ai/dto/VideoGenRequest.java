package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for video generation.
 *
 * @param prompt text description of the video to generate (required)
 * @param model preferred model (optional)
 * @param imageUrl optional reference image URL for image-to-video
 * @param durationSeconds desired video duration in seconds (optional)
 * @param aspectRatio aspect ratio (e.g., "16:9", "9:16", "1:1") (optional)
 * @param resolution resolution (e.g., "720p", "1080p") (optional)
 */
public record VideoGenRequest(
        @NotBlank(message = "prompt is required")
        String prompt,
        String model,
        String imageUrl,
        Integer durationSeconds,
        String aspectRatio,
        String resolution
) {
    /** Convenience constructor for simple prompt-only generation. */
    public VideoGenRequest(String prompt, String model) {
        this(prompt, model, null, null, null, null);
    }
}
