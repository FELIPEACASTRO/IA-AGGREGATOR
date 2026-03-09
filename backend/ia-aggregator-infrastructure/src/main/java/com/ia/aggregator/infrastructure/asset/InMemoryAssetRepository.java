package com.ia.aggregator.infrastructure.asset;

import com.ia.aggregator.application.asset.port.out.AssetRepository;
import com.ia.aggregator.domain.asset.Asset;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of AssetRepository.
 * Will be replaced by JPA implementation when content.assets table is created (Phase 2).
 */
@Repository
public class InMemoryAssetRepository implements AssetRepository {

    private final Map<UUID, Asset> store = new ConcurrentHashMap<>();
    private final Map<UUID, Long> usageCounters = new ConcurrentHashMap<>();
    private final Map<UUID, List<Double>> costEntries = new ConcurrentHashMap<>();
    private final Map<UUID, long[]> successFailCounts = new ConcurrentHashMap<>();

    @Override
    public void save(Asset asset) {
        store.put(asset.id(), asset);
    }

    @Override
    public Optional<Asset> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Asset> findByOrg(UUID orgId, Asset.AssetScope scope, Asset.AssetType type,
                                  int offset, int limit) {
        return store.values().stream()
                .filter(a -> a.orgId().equals(orgId))
                .filter(a -> scope == null || a.scope() == scope)
                .filter(a -> type == null || a.type() == type)
                .sorted(Comparator.comparing(Asset::updatedAt).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> search(UUID orgId, String query, int offset, int limit) {
        String lowerQuery = query.toLowerCase();
        return store.values().stream()
                .filter(a -> a.orgId().equals(orgId))
                .filter(a -> a.name().toLowerCase().contains(lowerQuery)
                        || (a.description() != null && a.description().toLowerCase().contains(lowerQuery)))
                .sorted(Comparator.comparing(Asset::updatedAt).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        store.remove(id);
        usageCounters.remove(id);
        costEntries.remove(id);
        successFailCounts.remove(id);
    }

    @Override
    public void incrementUsage(UUID id) {
        usageCounters.merge(id, 1L, Long::sum);
        Asset existing = store.get(id);
        if (existing != null) {
            store.put(id, new Asset(
                    existing.id(), existing.orgId(), existing.createdBy(),
                    existing.name(), existing.description(), existing.type(), existing.scope(),
                    existing.content(), existing.variables(), existing.metadata(),
                    usageCounters.getOrDefault(id, 0L), existing.averageRating(),
                    existing.published(), existing.createdAt(), Instant.now()
            ));
        }
    }

    @Override
    public double getTotalCost(UUID id) {
        List<Double> costs = costEntries.get(id);
        if (costs == null || costs.isEmpty()) return 0.0;
        return costs.stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public double getSuccessRate(UUID id) {
        long[] counts = successFailCounts.get(id);
        if (counts == null || (counts[0] + counts[1]) == 0) return 1.0;
        return (double) counts[0] / (counts[0] + counts[1]);
    }
}
