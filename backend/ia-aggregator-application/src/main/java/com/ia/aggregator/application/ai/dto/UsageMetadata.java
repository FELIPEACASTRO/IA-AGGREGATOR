package com.ia.aggregator.application.ai.dto;

/**
 * Token usage and estimated cost information returned with AI responses.
 *
 * <p>All fields are nullable — providers may not expose all usage data.
 * Tokens are never hardcoded prices; {@code estimatedCostUsd} is advisory only.
 *
 * @param inputTokens    tokens consumed from the prompt (prompt tokens)
 * @param outputTokens   tokens generated in the response (completion tokens)
 * @param totalTokens    sum of input + output (may differ if provider includes system tokens)
 * @param estimatedCostUsd advisory cost estimate in USD based on known pricing; null if unknown
 */
public record UsageMetadata(
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        Double estimatedCostUsd
) {
    /** Zero-usage metadata for providers that do not expose token counts. */
    public static final UsageMetadata UNKNOWN = new UsageMetadata(null, null, null, null);
}
