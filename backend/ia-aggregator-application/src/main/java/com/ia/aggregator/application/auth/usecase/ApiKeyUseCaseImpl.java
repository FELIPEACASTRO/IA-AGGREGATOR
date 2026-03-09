package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.port.in.ApiKeyUseCase;
import com.ia.aggregator.application.auth.port.out.ApiKeyRepository;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * API key management with SHA-256 hashing.
 *
 * <p>Keys are stored as hashes; the raw key is only shown once at creation.
 * Format: iagg_{32-char-random} (prefix for identification).
 */
@Service
public class ApiKeyUseCaseImpl implements ApiKeyUseCase {

    private static final String KEY_PREFIX = "iagg_";
    private static final int KEY_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyUseCaseImpl(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public ApiKeyResult create(UUID userId, String name, List<String> scopes) {
        String rawKey = generateRawKey();
        String hash = hashKey(rawKey);
        String prefix = rawKey.substring(0, KEY_PREFIX.length() + 8);

        UUID keyId = UUID.randomUUID();
        apiKeyRepository.save(keyId, userId, name, hash, prefix, scopes, Instant.now());

        return new ApiKeyResult(keyId, name, rawKey, prefix, scopes);
    }

    @Override
    public List<ApiKeyInfo> listByUser(UUID userId) {
        return apiKeyRepository.findByUser(userId);
    }

    @Override
    public void revoke(UUID userId, UUID keyId) {
        apiKeyRepository.revoke(userId, keyId);
    }

    @Override
    public UUID validate(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            throw new BusinessException(ErrorCode.AUTH_004, "Invalid API key format");
        }

        String hash = hashKey(rawKey);
        return apiKeyRepository.findUserByHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004, "Invalid API key"));
    }

    private String generateRawKey() {
        byte[] bytes = new byte[KEY_LENGTH];
        RANDOM.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
