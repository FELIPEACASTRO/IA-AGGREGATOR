package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for structured document parsing.
 *
 * @param documentUrl URL of the document to parse (required if documentBase64 is null)
 * @param documentBase64 base64-encoded document content (required if documentUrl is null)
 * @param fileName original file name with extension (optional, helps with format detection)
 * @param outputFormat desired output format: "markdown", "json", "text" (default: "markdown")
 * @param provider preferred provider (optional)
 */
public record DocumentParsingRequest(
        String documentUrl,
        String documentBase64,
        String fileName,
        String outputFormat,
        String provider
) {}
