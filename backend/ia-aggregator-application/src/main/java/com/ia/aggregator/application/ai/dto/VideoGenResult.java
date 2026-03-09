package com.ia.aggregator.application.ai.dto;

/**
 * Result DTO for video generation.
 * Supports both synchronous (videoUrl set) and async (jobId set) responses.
 *
 * @param videoUrl URL to the generated video (null if async and still processing)
 * @param jobId async job identifier for polling (null if synchronous/completed)
 * @param status job status: "completed", "processing", "failed" (for async)
 * @param modelUsed actual model used
 * @param providerUsed provider that generated the video
 * @param estimatedSeconds estimated time remaining in seconds (for async)
 */
public record VideoGenResult(
        String videoUrl,
        String jobId,
        String status,
        String modelUsed,
        String providerUsed,
        Integer estimatedSeconds
) {
    /** Convenience constructor for completed synchronous result. */
    public VideoGenResult(String videoUrl, String modelUsed, String providerUsed) {
        this(videoUrl, null, "completed", modelUsed, providerUsed, null);
    }
}
