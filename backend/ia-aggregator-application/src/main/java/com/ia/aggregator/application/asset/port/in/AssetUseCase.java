package com.ia.aggregator.application.asset.port.in;

import com.ia.aggregator.domain.asset.Asset;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for asset library management (prompts, templates, presets).
 */
public interface AssetUseCase {

    Asset create(UUID orgId, UUID userId, String name, String description,
                 Asset.AssetType type, Asset.AssetScope scope, String content,
                 Map<String, String> variables);

    Asset getById(UUID assetId);

    List<Asset> listByOrg(UUID orgId, Asset.AssetScope scope, Asset.AssetType type,
                           int page, int size);

    List<Asset> search(UUID orgId, String query, int page, int size);

    Asset update(UUID assetId, String name, String description, String content,
                 Map<String, String> variables);

    Asset publish(UUID assetId);

    String renderTemplate(UUID assetId, Map<String, String> variableValues);

    AssetMetrics getMetrics(UUID assetId);

    void delete(UUID assetId);

    record AssetMetrics(long usageCount, double averageRating, double totalCostUsd,
                        double successRate) {}
}
