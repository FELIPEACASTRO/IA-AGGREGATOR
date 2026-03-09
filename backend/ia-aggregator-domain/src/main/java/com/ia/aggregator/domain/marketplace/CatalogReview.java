package com.ia.aggregator.domain.marketplace;

import java.time.Instant;
import java.util.UUID;

/**
 * User review/rating for a catalog entry.
 */
public record CatalogReview(
        UUID id,
        UUID entryId,
        UUID userId,
        int rating,
        String comment,
        Instant createdAt
) {}
