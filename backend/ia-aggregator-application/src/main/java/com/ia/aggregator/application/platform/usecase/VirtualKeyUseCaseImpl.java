package com.ia.aggregator.application.platform.usecase;

import com.ia.aggregator.application.platform.port.in.VirtualKeyUseCase;
import com.ia.aggregator.application.platform.port.out.VirtualKeyRepository;
import com.ia.aggregator.domain.platform.VirtualKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class VirtualKeyUseCaseImpl implements VirtualKeyUseCase {

    private static final Logger log = LoggerFactory.getLogger(VirtualKeyUseCaseImpl.class);
    private static final String KEY_PREFIX = "iagg_vk_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VirtualKeyRepository keyRepository;

    public VirtualKeyUseCaseImpl(VirtualKeyRepository keyRepository) {
        this.keyRepository = keyRepository;
    }

    @Override
    public VirtualKeyResult create(UUID orgId, UUID userId, String name,
                                    List<String> allowedModels, List<String> allowedCapabilities,
                                    int rateLimitRpm, double budgetLimitUsd, Instant expiresAt) {
        byte[] keyBytes = new byte[32];
        RANDOM.nextBytes(keyBytes);
        String rawKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String keyHash = sha256(rawKey);
        String prefix = rawKey.substring(0, KEY_PREFIX.length() + 8);

        Instant now = Instant.now();
        VirtualKey key = new VirtualKey(
                UUID.randomUUID(), orgId, userId, name, keyHash, prefix,
                allowedModels, allowedCapabilities, rateLimitRpm, budgetLimitUsd,
                0.0, true, now, null, expiresAt
        );
        keyRepository.save(key);
        log.info("Virtual key created: id={}, name={}, prefix={}", key.id(), name, prefix);
        return new VirtualKeyResult(key.id(), rawKey, prefix);
    }

    @Override
    public VirtualKey getById(UUID keyId) {
        return keyRepository.findById(keyId)
                .orElseThrow(() -> new NoSuchElementException("Virtual key not found: " + keyId));
    }

    @Override
    public List<VirtualKey> listByOrg(UUID orgId) {
        return keyRepository.findByOrg(orgId);
    }

    @Override
    public VirtualKey validate(String rawKey) {
        String hash = sha256(rawKey);
        VirtualKey key = keyRepository.findByKeyHash(hash)
                .orElseThrow(() -> new SecurityException("Invalid virtual key"));
        if (!key.enabled()) throw new SecurityException("Virtual key is disabled");
        if (key.expiresAt() != null && Instant.now().isAfter(key.expiresAt())) {
            throw new SecurityException("Virtual key has expired");
        }
        if (key.spentUsd() >= key.budgetLimitUsd()) {
            throw new SecurityException("Virtual key budget exceeded");
        }
        return key;
    }

    @Override
    public void revoke(UUID keyId) {
        VirtualKey key = getById(keyId);
        VirtualKey revoked = new VirtualKey(
                key.id(), key.orgId(), key.createdBy(), key.name(),
                key.keyHash(), key.keyPrefix(), key.allowedModels(),
                key.allowedCapabilities(), key.rateLimitRpm(), key.budgetLimitUsd(),
                key.spentUsd(), false, key.createdAt(), key.lastUsedAt(), key.expiresAt()
        );
        keyRepository.save(revoked);
        log.info("Virtual key revoked: id={}", keyId);
    }

    @Override
    public void recordUsage(UUID keyId, double costUsd) {
        VirtualKey key = getById(keyId);
        VirtualKey updated = new VirtualKey(
                key.id(), key.orgId(), key.createdBy(), key.name(),
                key.keyHash(), key.keyPrefix(), key.allowedModels(),
                key.allowedCapabilities(), key.rateLimitRpm(), key.budgetLimitUsd(),
                key.spentUsd() + costUsd, key.enabled(), key.createdAt(),
                Instant.now(), key.expiresAt()
        );
        keyRepository.save(updated);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
