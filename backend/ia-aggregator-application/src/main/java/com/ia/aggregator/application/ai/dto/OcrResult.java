package com.ia.aggregator.application.ai.dto;

import java.util.List;

/**
 * Result DTO for OCR processing.
 *
 * @param text full extracted text from the image
 * @param modelUsed actual model used
 * @param providerUsed provider that performed OCR
 * @param blocks text blocks with bounding box information (optional)
 * @param confidence overall OCR confidence [0.0, 1.0] (optional)
 */
public record OcrResult(
        String text,
        String modelUsed,
        String providerUsed,
        List<TextBlock> blocks,
        Double confidence
) {
    /** Convenience constructor without block-level detail. */
    public OcrResult(String text, String modelUsed, String providerUsed) {
        this(text, modelUsed, providerUsed, null, null);
    }

    /**
     * A text block with bounding box.
     *
     * @param text the text in this block
     * @param boundingBox bounding box coordinates [x, y, width, height]
     * @param confidence block-level confidence
     */
    public record TextBlock(String text, List<Integer> boundingBox, Double confidence) {
    }
}
