package com.ia.aggregator.application.ai.dto;

/**
 * A single threat intelligence search hit.
 *
 * @param title result title or subject
 * @param url source URL (may be .onion or clearnet)
 * @param snippet text excerpt
 * @param sourceType source category (e.g., "forum", "marketplace", "paste", "leak")
 * @param severity severity level (e.g., "low", "medium", "high", "critical")
 * @param discoveredDate date discovered ISO-8601
 * @param score relevance score (optional)
 */
public record ThreatIntelHit(
        String title,
        String url,
        String snippet,
        String sourceType,
        String severity,
        String discoveredDate,
        Double score
) {
}
