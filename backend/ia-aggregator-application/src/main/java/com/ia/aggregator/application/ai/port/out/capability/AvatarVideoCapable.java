package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.AvatarVideoRequest;
import com.ia.aggregator.application.ai.dto.AvatarVideoResult;

/**
 * Capability interface for avatar video generation.
 * Providers implementing this can generate videos with AI-driven avatars.
 */
public interface AvatarVideoCapable {

    /**
     * Generates an avatar video from a text script.
     *
     * @param request the avatar video request with script and avatar configuration
     * @return the result with video download URL and duration
     */
    AvatarVideoResult generateAvatarVideo(AvatarVideoRequest request);
}
