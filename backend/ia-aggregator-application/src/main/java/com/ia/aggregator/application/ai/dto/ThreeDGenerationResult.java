package com.ia.aggregator.application.ai.dto;

/**
 * Result DTO for 3D model generation.
 *
 * @param modelUrl URL to download the generated 3D model
 * @param thumbnailUrl URL of the preview thumbnail (nullable)
 * @param format output format of the model (e.g., "glb", "fbx")
 * @param provider the provider that generated the model
 * @param usage processing usage metadata (nullable)
 */
public record ThreeDGenerationResult(
        String modelUrl,
        String thumbnailUrl,
        String format,
        String provider,
        UsageMetadata usage
) {}
