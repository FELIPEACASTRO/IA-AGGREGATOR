package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.VideoGenRequest;
import com.ia.aggregator.application.ai.dto.VideoGenResult;

/**
 * Capability interface for text/image-to-video generation.
 * Most video providers use async polling (submit job -> poll status -> get result).
 */
public interface VideoGenerationCapable {

    /**
     * Submits a video generation request. For async providers, returns a job status
     * that can be polled until completion.
     *
     * @param request the video generation request with prompt and parameters
     * @return the result containing video URL or async job status
     */
    VideoGenResult generateVideo(VideoGenRequest request);
}
