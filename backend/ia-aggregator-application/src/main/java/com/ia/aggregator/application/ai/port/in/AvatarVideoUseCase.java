package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.AvatarVideoRequest;
import com.ia.aggregator.application.ai.dto.AvatarVideoResult;

/**
 * Use case port for avatar video generation.
 */
public interface AvatarVideoUseCase {

    AvatarVideoResult execute(AvatarVideoRequest request);
}
