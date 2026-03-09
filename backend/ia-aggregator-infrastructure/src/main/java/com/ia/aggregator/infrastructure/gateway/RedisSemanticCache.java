package com.ia.aggregator.infrastructure.gateway;

import com.ia.aggregator.application.gateway.port.out.SemanticCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/**
 * Redis-backed semantic cache for LLM responses.
 *
 * <p>Uses SHA-256 hash of prompt+model+params as cache key.
 * Stores responses with configurable TTL.
 */
@Component
public class RedisSemanticCache implements SemanticCachePort {

    private static final Logger log = LoggerFactory.getLogger(RedisSemanticCache.class);
    private static final String CACHE_PREFIX = "semantic_cache:";

    private final StringRedisTemplate redisTemplate;

    public RedisSemanticCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<String> get(String cacheKey) {
        try {
            String value = redisTemplate.opsForValue().get(CACHE_PREFIX + cacheKey);
            if (value != null) {
                log.debug("Semantic cache hit: {}", cacheKey);
                return Optional.of(value);
            }
        } catch (Exception e) {
            log.warn("Semantic cache get failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void put(String cacheKey, String response, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(CACHE_PREFIX + cacheKey, response, ttl);
            log.debug("Semantic cache put: {} (ttl={}s)", cacheKey, ttl.toSeconds());
        } catch (Exception e) {
            log.warn("Semantic cache put failed: {}", e.getMessage());
        }
    }

    @Override
    public String computeKey(String prompt, String model, String params) {
        String input = prompt + "|" + model + "|" + (params != null ? params : "");
        return sha256(input);
    }

    @Override
    public void evict(String cacheKey) {
        try {
            redisTemplate.delete(CACHE_PREFIX + cacheKey);
        } catch (Exception e) {
            log.warn("Semantic cache evict failed: {}", e.getMessage());
        }
    }

    @Override
    public void evictAll() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Semantic cache cleared: {} entries", keys.size());
            }
        } catch (Exception e) {
            log.warn("Semantic cache evictAll failed: {}", e.getMessage());
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
