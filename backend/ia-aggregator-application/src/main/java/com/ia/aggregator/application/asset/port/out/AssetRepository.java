package com.ia.aggregator.application.asset.port.out;

import com.ia.aggregator.domain.asset.Asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for asset persistence.
 */
public interface AssetRepository {

    void save(Asset asset);

    Optional<Asset> findById(UUID id);

    List<Asset> findByOrg(UUID orgId, Asset.AssetScope scope, Asset.AssetType type,
                           int offset, int limit);

    List<Asset> search(UUID orgId, String query, int offset, int limit);

    void delete(UUID id);

    void incrementUsage(UUID id);

    /**
     * Returns aggregated cost for an asset across all usages.
     * Returns 0.0 if no cost data is available.
     */
    double getTotalCost(UUID id);

    /**
     * Returns the success rate (0.0 to 1.0) for an asset's executions.
     * Returns 1.0 if no execution data is available (no failures recorded).
     */
    double getSuccessRate(UUID id);
}
