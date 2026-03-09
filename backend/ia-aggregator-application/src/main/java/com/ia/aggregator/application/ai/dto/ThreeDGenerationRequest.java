package com.ia.aggregator.application.ai.dto;

/**
 * Request DTO for 3D model generation.
 *
 * @param prompt text description of the 3D model to generate (optional if imageUrl is provided)
 * @param imageUrl reference image URL for image-to-3D (optional if prompt is provided)
 * @param outputFormat desired output format: "glb", "fbx", "obj", "usdz" (default: "glb")
 * @param provider preferred provider (optional)
 * @param model preferred model (optional)
 */
public record ThreeDGenerationRequest(
        String prompt,
        String imageUrl,
        String outputFormat,
        String provider,
        String model
) {}
