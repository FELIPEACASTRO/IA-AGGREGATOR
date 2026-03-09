package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for avatar video generation.
 *
 * @param script the text script for the avatar to speak (required)
 * @param avatarId identifier of the avatar to use (provider-specific)
 * @param voiceId identifier of the voice to use (provider-specific, optional)
 * @param language language code for speech synthesis (optional, default: "en")
 * @param provider preferred provider (optional)
 */
public record AvatarVideoRequest(
        @NotBlank(message = "script is required")
        String script,
        String avatarId,
        String voiceId,
        String language,
        String provider
) {}
