package com.ia.aggregator.application.ai.dto;

/**
 * Status of an asynchronous provider job (used by media/audio providers with polling).
 *
 * @param jobId the unique job identifier
 * @param status current status: "queued", "processing", "completed", "failed"
 * @param resultUrl URL to the completed result (null if not yet completed)
 * @param errorMessage error details if failed (null otherwise)
 * @param progress progress percentage 0-100 (null if not reported)
 * @param estimatedSeconds estimated time remaining (null if unknown)
 */
public record AsyncJobStatus(
        String jobId,
        String status,
        String resultUrl,
        String errorMessage,
        Integer progress,
        Integer estimatedSeconds
) {
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status);
    }

    public boolean isProcessing() {
        return "queued".equals(status) || "processing".equals(status);
    }
}
