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
}
