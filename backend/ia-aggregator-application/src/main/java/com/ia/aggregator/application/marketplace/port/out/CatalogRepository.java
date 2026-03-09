package com.ia.aggregator.application.marketplace.port.out;

import com.ia.aggregator.domain.marketplace.CatalogEntry;
import com.ia.aggregator.domain.marketplace.CatalogReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogRepository {
    void save(CatalogEntry entry);
    Optional<CatalogEntry> findById(UUID id);
    List<CatalogEntry> search(String query, CatalogEntry.EntryType type,
                               List<String> categories, int offset, int limit);
    List<CatalogEntry> findPopular(int offset, int limit);
    void saveReview(CatalogReview review);
    List<CatalogReview> findReviews(UUID entryId, int offset, int limit);
    double averageRating(UUID entryId);
}
