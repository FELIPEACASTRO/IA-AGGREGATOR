package com.ia.aggregator.infrastructure.marketplace.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.marketplace.port.out.CatalogRepository;
import com.ia.aggregator.domain.marketplace.CatalogEntry;
import com.ia.aggregator.domain.marketplace.CatalogReview;
import com.ia.aggregator.infrastructure.marketplace.persistence.entity.CatalogEntryJpaEntity;
import com.ia.aggregator.infrastructure.marketplace.persistence.entity.CatalogReviewJpaEntity;
import com.ia.aggregator.infrastructure.marketplace.persistence.repository.CatalogEntryJpaRepository;
import com.ia.aggregator.infrastructure.marketplace.persistence.repository.CatalogReviewJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CatalogRepositoryImpl implements CatalogRepository {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final CatalogEntryJpaRepository entryJpa;
    private final CatalogReviewJpaRepository reviewJpa;

    public CatalogRepositoryImpl(CatalogEntryJpaRepository entryJpa,
                                 CatalogReviewJpaRepository reviewJpa) {
        this.entryJpa = entryJpa;
        this.reviewJpa = reviewJpa;
    }

    @Override
    public void save(CatalogEntry entry) {
        CatalogEntryJpaEntity entity = entryJpa.findById(entry.id())
                .orElseGet(CatalogEntryJpaEntity::new);
        entity.setId(entry.id());
        entity.setPublisherId(entry.publisherId());
        entity.setName(entry.name());
        entity.setDescription(entry.description());
        entity.setEntryType(entry.type() != null ? entry.type().name() : null);
        entity.setCategories(entry.categories() != null
                ? entry.categories().toArray(new String[0])
                : new String[0]);
        entity.setTags(entry.tags() != null
                ? entry.tags().toArray(new String[0])
                : new String[0]);
        entity.setVersion(entry.version());
        entity.setPrice(java.math.BigDecimal.valueOf(entry.price()));
        entity.setRevenueSharePercent(java.math.BigDecimal.valueOf(entry.revenueSharePercent()));
        entity.setAverageRating(java.math.BigDecimal.valueOf(entry.averageRating()));
        entity.setInstallCount(entry.installCount());
        entity.setStatus(entry.status() != null ? entry.status().name() : "DRAFT");
        entity.setMetadata(toJson(entry.metadata()));
        entity.setPublishedAt(entry.publishedAt());
        entryJpa.save(entity);
    }

    @Override
    public Optional<CatalogEntry> findById(UUID id) {
        return entryJpa.findById(id).map(this::toEntryDomain);
    }

    @Override
    public List<CatalogEntry> search(String query, CatalogEntry.EntryType type,
                                      List<String> categories, int offset, int limit) {
        int safeLimit = Math.max(1, limit);
        int page = offset / safeLimit;
        String entryType = type != null ? type.name() : null;
        return entryJpa.search(query, entryType, PageRequest.of(page, safeLimit)).stream()
                .map(this::toEntryDomain)
                .filter(entry -> categories == null || categories.isEmpty()
                        || entry.categories().stream().anyMatch(categories::contains))
                .toList();
    }

    @Override
    public List<CatalogEntry> findPopular(int offset, int limit) {
        int safeLimit = Math.max(1, limit);
        int page = offset / safeLimit;
        return entryJpa.findPopular(PageRequest.of(page, safeLimit)).stream()
                .map(this::toEntryDomain)
                .toList();
    }

    @Override
    public void saveReview(CatalogReview review) {
        CatalogReviewJpaEntity entity = reviewJpa.findById(review.id())
                .orElseGet(CatalogReviewJpaEntity::new);
        entity.setId(review.id());
        entity.setEntryId(review.entryId());
        entity.setUserId(review.userId());
        entity.setRating(review.rating());
        entity.setComment(review.comment());
        reviewJpa.save(entity);
    }

    @Override
    public List<CatalogReview> findReviews(UUID entryId, int offset, int limit) {
        int safeLimit = Math.max(1, limit);
        int page = offset / safeLimit;
        return reviewJpa.findByEntryIdOrderByCreatedAtDesc(entryId, PageRequest.of(page, safeLimit))
                .stream()
                .map(this::toReviewDomain)
                .toList();
    }

    @Override
    public double averageRating(UUID entryId) {
        return reviewJpa.averageRatingByEntryId(entryId);
    }

    private CatalogEntry toEntryDomain(CatalogEntryJpaEntity entity) {
        CatalogEntry.EntryType entryType;
        try {
            entryType = CatalogEntry.EntryType.valueOf(entity.getEntryType());
        } catch (Exception e) {
            entryType = CatalogEntry.EntryType.TOOL;
        }

        CatalogEntry.CatalogStatus status;
        try {
            status = CatalogEntry.CatalogStatus.valueOf(entity.getStatus());
        } catch (Exception e) {
            status = CatalogEntry.CatalogStatus.DRAFT;
        }

        List<String> categories = entity.getCategories() != null
                ? Arrays.asList(entity.getCategories())
                : List.of();

        List<String> tags = entity.getTags() != null
                ? Arrays.asList(entity.getTags())
                : List.of();

        return new CatalogEntry(
                entity.getId(),
                entity.getPublisherId(),
                entity.getName(),
                entity.getDescription(),
                entryType,
                categories,
                tags,
                entity.getVersion(),
                entity.getPrice().doubleValue(),
                entity.getRevenueSharePercent().doubleValue(),
                entity.getAverageRating().doubleValue(),
                entity.getInstallCount(),
                status,
                fromJson(entity.getMetadata()),
                entity.getPublishedAt(),
                entity.getUpdatedAt()
        );
    }

    private CatalogReview toReviewDomain(CatalogReviewJpaEntity entity) {
        return new CatalogReview(
                entity.getId(),
                entity.getEntryId(),
                entity.getUserId(),
                entity.getRating(),
                entity.getComment(),
                entity.getCreatedAt()
        );
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map != null ? map : Map.of());
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) return Map.of();
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
