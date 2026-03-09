package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.TranslationRequest;
import com.ia.aggregator.application.ai.dto.TranslationResult;

/**
 * Use case port for text translation.
 */
public interface TranslationUseCase {

    TranslationResult execute(TranslationRequest request);
}
