package com.ia.aggregator.application.marketplace.port.in;

import com.ia.aggregator.domain.marketplace.CatalogEntry;
import com.ia.aggregator.domain.marketplace.CatalogReview;

import java.util.List;
import java.util.UUID;

/**
 * Use case for catalog and marketplace management.
 */
public interface CatalogUseCase {

    CatalogEntry publish(CatalogEntry entry);

    CatalogEntry getById(UUID entryId);

    List<CatalogEntry> search(String query, CatalogEntry.EntryType type,
                               List<String> categories, int page, int size);

    List<CatalogEntry> listPopular(int page, int size);

    CatalogEntry approve(UUID entryId);

    CatalogEntry reject(UUID entryId, String reason);

    CatalogEntry deprecate(UUID entryId);

    void install(UUID entryId, UUID orgId);

    CatalogReview addReview(UUID entryId, UUID userId, int rating, String comment);

    List<CatalogReview> getReviews(UUID entryId, int page, int size);

    double getAverageRating(UUID entryId);
}
