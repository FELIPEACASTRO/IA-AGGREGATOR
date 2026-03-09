package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.application.ai.dto.AsyncJobStatus;

/**
 * SPI for async job lifecycle management — submit, poll, cancel, retrieve result.
 *
 * <p>Implemented by providers whose operations are inherently async (IMAGE_GENERATION,
 * VIDEO_GENERATION, some STT providers). The pattern is:
 * <pre>
 *   jobId = submit(payload)
 *   while (!done) { status = poll(jobId); sleep(pollInterval); }
 *   result = getResult(jobId)
 * </pre>
 *
 * <p>IMPORTANT: Do NOT apply retry at the submit() level for expensive media jobs —
 * duplicate submissions result in duplicate charges.
 * Retry may be applied to poll() for transient network failures only.
 *
 * <p>Big O: O(P) poll iterations where P <= maxPollAttempts (bounded).
 */
public interface AsyncJobHandle {

    /**
     * Submits a job and returns the provider-assigned job ID.
     *
     * @param payload the serialized request body for the provider's submit endpoint
     * @return job ID string (format varies by provider)
     */
    String submit(Object payload);

    /**
     * Polls the current status of a job.
     *
     * @param jobId the job ID returned by {@link #submit}
     * @return current status (completed / processing / failed)
     */
    AsyncJobStatus poll(String jobId);

    /**
     * Requests cancellation of a job. Best-effort — not all providers support cancellation.
     * Implementations should not throw if the provider does not support cancel.
     *
     * @param jobId the job to cancel
     */
    void cancel(String jobId);

    /**
     * Retrieves the final result URL or content once the job status is {@code completed}.
     * Should only be called after {@link #poll} returns a completed status.
     *
     * @param jobId the completed job ID
     * @return the result URL or content identifier
     */
    String getResult(String jobId);
}
