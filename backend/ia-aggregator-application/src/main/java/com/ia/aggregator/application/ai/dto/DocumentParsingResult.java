package com.ia.aggregator.application.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * Result DTO for structured document parsing.
 *
 * @param parsedContent the parsed content in the requested output format
 * @param pageCount number of pages processed
 * @param tables extracted tables as list of row-maps (nullable)
 * @param metadata additional extraction metadata (nullable)
 * @param provider the provider that performed the parsing
 * @param usage processing usage metadata (nullable)
 */
public record DocumentParsingResult(
        String parsedContent,
        int pageCount,
        List<Map<String, String>> tables,
        Map<String, Object> metadata,
        String provider,
        UsageMetadata usage
) {}
