package com.ia.aggregator.application.ai.dto;

/**
 * Request DTO for optical character recognition.
 *
 * @param imageData image bytes for OCR (mutually exclusive with imageUrl)
 * @param imageUrl image URL for OCR (mutually exclusive with imageData)
 * @param model preferred OCR model (optional)
 * @param language hint for the language in the image (optional)
 */
public record OcrRequest(
        byte[] imageData,
        String imageUrl,
        String model,
        String language
) {
    /** Convenience constructor for URL-based OCR. */
    public OcrRequest(String imageUrl) {
        this(null, imageUrl, null, null);
    }

    /** Convenience constructor for bytes-based OCR. */
    public OcrRequest(byte[] imageData) {
        this(imageData, null, null, null);
    }
}
