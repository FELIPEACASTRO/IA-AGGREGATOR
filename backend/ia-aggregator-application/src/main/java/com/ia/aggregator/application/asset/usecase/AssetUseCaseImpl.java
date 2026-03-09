package com.ia.aggregator.application.asset.usecase;

import com.ia.aggregator.application.asset.port.in.AssetUseCase;
import com.ia.aggregator.application.asset.port.out.AssetRepository;
import com.ia.aggregator.domain.asset.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssetUseCaseImpl implements AssetUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssetUseCaseImpl.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final AssetRepository assetRepository;

    public AssetUseCaseImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public Asset create(UUID orgId, UUID userId, String name, String description,
                        Asset.AssetType type, Asset.AssetScope scope, String content,
                        Map<String, String> variables) {
        Instant now = Instant.now();
        Asset asset = new Asset(
                UUID.randomUUID(), orgId, userId, name, description, type, scope,
                content, variables != null ? variables : Map.of(), Map.of(),
                0, 0.0, false, now, now
        );
        assetRepository.save(asset);
        log.info("Asset created: id={}, name={}, type={}, scope={}", asset.id(), name, type, scope);
        return asset;
    }

    @Override
    public Asset getById(UUID assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));
    }

    @Override
    public List<Asset> listByOrg(UUID orgId, Asset.AssetScope scope, Asset.AssetType type,
                                  int page, int size) {
        return assetRepository.findByOrg(orgId, scope, type, page * size, size);
    }

    @Override
    public List<Asset> search(UUID orgId, String query, int page, int size) {
        return assetRepository.search(orgId, query, page * size, size);
    }

    @Override
    public Asset update(UUID assetId, String name, String description, String content,
                        Map<String, String> variables) {
        Asset existing = getById(assetId);
        Asset updated = new Asset(
                existing.id(), existing.orgId(), existing.createdBy(),
                name != null ? name : existing.name(),
                description != null ? description : existing.description(),
                existing.type(), existing.scope(),
                content != null ? content : existing.content(),
                variables != null ? variables : existing.variables(),
                existing.metadata(), existing.usageCount(), existing.averageRating(),
                existing.published(), existing.createdAt(), Instant.now()
        );
        assetRepository.save(updated);
        return updated;
    }

    @Override
    public Asset publish(UUID assetId) {
        Asset existing = getById(assetId);
        Asset published = new Asset(
                existing.id(), existing.orgId(), existing.createdBy(),
                existing.name(), existing.description(), existing.type(), existing.scope(),
                existing.content(), existing.variables(), existing.metadata(),
                existing.usageCount(), existing.averageRating(), true,
                existing.createdAt(), Instant.now()
        );
        assetRepository.save(published);
        return published;
    }

    @Override
    public String renderTemplate(UUID assetId, Map<String, String> variableValues) {
        Asset asset = getById(assetId);
        assetRepository.incrementUsage(assetId);

        String rendered = asset.content();
        Matcher matcher = VARIABLE_PATTERN.matcher(rendered);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variableValues.getOrDefault(varName,
                    asset.variables().getOrDefault(varName, "{{" + varName + "}}"));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public AssetMetrics getMetrics(UUID assetId) {
        Asset asset = getById(assetId);
        return new AssetMetrics(asset.usageCount(), asset.averageRating(), 0.0, 0.0);
    }

    @Override
    public void delete(UUID assetId) {
        assetRepository.delete(assetId);
        log.info("Asset deleted: id={}", assetId);
    }
}
