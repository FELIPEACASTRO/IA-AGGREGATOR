package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.TranslationRequest;
import com.ia.aggregator.application.ai.dto.TranslationResult;

/**
 * Capability interface for text translation.
 * Providers implementing this can translate text between languages.
 */
public interface TranslationCapable {

    /**
     * Translates text from source language to target language.
     *
     * @param request the translation request
     * @return the translation result with translated text and metadata
     */
    TranslationResult translate(TranslationRequest request);
}
