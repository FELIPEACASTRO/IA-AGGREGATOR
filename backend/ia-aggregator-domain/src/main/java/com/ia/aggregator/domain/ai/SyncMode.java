package com.ia.aggregator.domain.ai;

/**
 * Describes how a provider's operation completes — synchronously, via async polling,
 * or via streaming.
 *
 * <p>Used by {@code ProviderDescriptor} and routing logic to choose the correct execution path.
 * Big O: O(1) for all enum operations.
 */
public enum SyncMode {

    /**
     * Response is returned inline in the HTTP response body.
     * Used by: CHAT, EMBEDDINGS, RERANK, OCR, SEARCH, most audio providers.
     */
    SYNCHRONOUS,

    /**
     * Job is submitted and a job ID is returned. Client must poll a status endpoint.
     * Used by: IMAGE_GENERATION (Stability, fal.ai, Replicate, BFL, Runway, Ideogram),
     * VIDEO_GENERATION, some STT providers (AssemblyAI, Gladia).
     * NOTE: Do NOT apply retry to async jobs — retry risks duplicate charges.
     */
    ASYNC_POLLING,

    /**
     * Response is streamed via SSE or chunked transfer.
     * Used by: CHAT with stream=true (planned for future).
     */
    STREAMING
}
