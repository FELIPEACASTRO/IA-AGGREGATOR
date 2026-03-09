package com.ia.aggregator.application.ai.dto;

/**
 * Result DTO for text translation.
 *
 * @param translatedText the translated text
 * @param detectedSourceLang detected source language (when auto-detect is used)
 * @param provider the provider that performed the translation
 * @param usage token/character usage metadata (nullable)
 */
public record TranslationResult(
        String translatedText,
        String detectedSourceLang,
        String provider,
        UsageMetadata usage
) {}
