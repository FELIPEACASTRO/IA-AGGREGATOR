package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.OcrRequest;
import com.ia.aggregator.application.ai.dto.OcrResult;

/**
 * Capability interface for optical character recognition (OCR).
 * Providers implementing this extract text from images.
 */
public interface OcrCapable {

    /**
     * Extracts text from an image using OCR.
     *
     * @param request the OCR request with image bytes or URL
     * @return the OCR result with extracted text and bounding boxes
     */
    OcrResult ocr(OcrRequest request);
}
