package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.VideoGenRequest;
import com.ia.aggregator.application.ai.dto.VideoGenResult;

/**
 * Input port for video generation.
 * Creates video content from text prompts or reference images.
 */
public interface VideoGenerationUseCase {
    VideoGenResult execute(VideoGenRequest request);
}
