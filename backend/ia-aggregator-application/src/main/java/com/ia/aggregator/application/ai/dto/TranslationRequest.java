package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for text translation.
 *
 * @param text the text to translate (required)
 * @param sourceLang source language code (optional — auto-detect if null)
 * @param targetLang target language code (required)
 * @param provider preferred provider (optional)
 * @param model preferred model (optional)
 */
public record TranslationRequest(
        @NotBlank(message = "text is required")
        String text,
        String sourceLang,
        @NotBlank(message = "targetLang is required")
        String targetLang,
        String provider,
        String model
) {}
