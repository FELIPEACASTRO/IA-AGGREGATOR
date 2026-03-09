package com.ia.aggregator.domain.marketplace;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * An entry in the public catalog / marketplace.
 */
public record CatalogEntry(
        UUID id,
        UUID publisherId,
        String name,
        String description,
        EntryType type,
        List<String> categories,
        List<String> tags,
        String version,
        double price,
        double revenueSharePercent,
        double averageRating,
        long installCount,
        CatalogStatus status,
        Map<String, String> metadata,
        Instant publishedAt,
        Instant updatedAt
) {
    public enum EntryType {
        MODEL, AGENT, TEMPLATE, CONNECTOR, WORKFLOW, TOOL
    }

    public enum CatalogStatus {
        DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, DEPRECATED
    }
}
