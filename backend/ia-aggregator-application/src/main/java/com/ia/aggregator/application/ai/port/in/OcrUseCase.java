package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.OcrRequest;
import com.ia.aggregator.application.ai.dto.OcrResult;

/**
 * Input port for optical character recognition (OCR).
 * Extracts text from images.
 */
public interface OcrUseCase {
    OcrResult execute(OcrRequest request);
}
