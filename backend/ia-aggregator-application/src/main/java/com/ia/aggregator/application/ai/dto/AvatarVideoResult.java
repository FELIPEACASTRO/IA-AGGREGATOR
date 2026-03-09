package com.ia.aggregator.application.ai.dto;

/**
 * Result DTO for avatar video generation.
 *
 * @param videoUrl URL to download the generated video
 * @param durationSeconds duration of the video in seconds
 * @param provider the provider that generated the video
 * @param usage processing usage metadata (nullable)
 */
public record AvatarVideoResult(
        String videoUrl,
        double durationSeconds,
        String provider,
        UsageMetadata usage
) {}
