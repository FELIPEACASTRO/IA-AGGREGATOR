package com.ia.aggregator.application.marketplace.usecase;

import com.ia.aggregator.application.marketplace.port.in.CatalogUseCase;
import com.ia.aggregator.application.marketplace.port.out.CatalogRepository;
import com.ia.aggregator.domain.marketplace.CatalogEntry;
import com.ia.aggregator.domain.marketplace.CatalogReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class CatalogUseCaseImpl implements CatalogUseCase {

    private static final Logger log = LoggerFactory.getLogger(CatalogUseCaseImpl.class);

    private final CatalogRepository catalogRepository;

    public CatalogUseCaseImpl(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Override
    public CatalogEntry publish(CatalogEntry entry) {
        Instant now = Instant.now();
        CatalogEntry withId = new CatalogEntry(
                entry.id() != null ? entry.id() : UUID.randomUUID(),
                entry.publisherId(), entry.name(), entry.description(),
                entry.type(), entry.categories(), entry.tags(),
                entry.version(), entry.price(), entry.revenueSharePercent(),
                0.0, 0, CatalogEntry.CatalogStatus.PENDING_REVIEW,
                entry.metadata(), now, now
        );
        catalogRepository.save(withId);
        log.info("Catalog entry published: id={}, name={}, type={}", withId.id(), withId.name(), withId.type());
        return withId;
    }

    @Override
    public CatalogEntry getById(UUID entryId) {
        return catalogRepository.findById(entryId)
                .orElseThrow(() -> new NoSuchElementException("Catalog entry not found: " + entryId));
    }

    @Override
    public List<CatalogEntry> search(String query, CatalogEntry.EntryType type,
                                      List<String> categories, int page, int size) {
        return catalogRepository.search(query, type, categories, page * size, size);
    }

    @Override
    public List<CatalogEntry> listPopular(int page, int size) {
        return catalogRepository.findPopular(page * size, size);
    }

    @Override
    public CatalogEntry approve(UUID entryId) {
        CatalogEntry entry = getById(entryId);
        CatalogEntry approved = new CatalogEntry(
                entry.id(), entry.publisherId(), entry.name(), entry.description(),
                entry.type(), entry.categories(), entry.tags(), entry.version(),
                entry.price(), entry.revenueSharePercent(), entry.averageRating(),
                entry.installCount(), CatalogEntry.CatalogStatus.PUBLISHED,
                entry.metadata(), entry.publishedAt(), Instant.now()
        );
        catalogRepository.save(approved);
        return approved;
    }

    @Override
    public CatalogEntry reject(UUID entryId, String reason) {
        CatalogEntry entry = getById(entryId);
        CatalogEntry rejected = new CatalogEntry(
                entry.id(), entry.publisherId(), entry.name(), entry.description(),
                entry.type(), entry.categories(), entry.tags(), entry.version(),
                entry.price(), entry.revenueSharePercent(), entry.averageRating(),
                entry.installCount(), CatalogEntry.CatalogStatus.REJECTED,
                entry.metadata(), entry.publishedAt(), Instant.now()
        );
        catalogRepository.save(rejected);
        return rejected;
    }

    @Override
    public CatalogEntry deprecate(UUID entryId) {
        CatalogEntry entry = getById(entryId);
        CatalogEntry deprecated = new CatalogEntry(
                entry.id(), entry.publisherId(), entry.name(), entry.description(),
                entry.type(), entry.categories(), entry.tags(), entry.version(),
                entry.price(), entry.revenueSharePercent(), entry.averageRating(),
                entry.installCount(), CatalogEntry.CatalogStatus.DEPRECATED,
                entry.metadata(), entry.publishedAt(), Instant.now()
        );
        catalogRepository.save(deprecated);
        return deprecated;
    }

    @Override
    public void install(UUID entryId, UUID orgId) {
        CatalogEntry entry = getById(entryId);
        CatalogEntry updated = new CatalogEntry(
                entry.id(), entry.publisherId(), entry.name(), entry.description(),
                entry.type(), entry.categories(), entry.tags(), entry.version(),
                entry.price(), entry.revenueSharePercent(), entry.averageRating(),
                entry.installCount() + 1, entry.status(),
                entry.metadata(), entry.publishedAt(), Instant.now()
        );
        catalogRepository.save(updated);
        log.info("Catalog entry installed: entryId={}, orgId={}", entryId, orgId);
    }

    @Override
    public CatalogReview addReview(UUID entryId, UUID userId, int rating, String comment) {
        CatalogReview review = new CatalogReview(
                UUID.randomUUID(), entryId, userId, rating, comment, Instant.now()
        );
        catalogRepository.saveReview(review);
        return review;
    }

    @Override
    public List<CatalogReview> getReviews(UUID entryId, int page, int size) {
        return catalogRepository.findReviews(entryId, page * size, size);
    }

    @Override
    public double getAverageRating(UUID entryId) {
        return catalogRepository.averageRating(entryId);
    }
}
