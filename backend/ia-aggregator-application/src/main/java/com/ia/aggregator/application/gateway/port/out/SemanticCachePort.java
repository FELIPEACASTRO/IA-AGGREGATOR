package com.ia.aggregator.application.gateway.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Semantic cache for LLM responses.
 *
 * <p>Caches responses by prompt hash to avoid duplicate API calls for
 * identical or semantically similar prompts.
 */
public interface SemanticCachePort {

    /**
     * Look up a cached response by cache key.
     *
     * @param cacheKey the cache key (typically a hash of prompt + model + params)
     * @return the cached response if found
     */
    Optional<String> get(String cacheKey);

    /**
     * Store a response in the cache.
     *
     * @param cacheKey the cache key
     * @param response the response to cache
     * @param ttl      time-to-live for the cache entry
     */
    void put(String cacheKey, String response, Duration ttl);

    /**
     * Generate a cache key from prompt, model, and parameters.
     *
     * @param prompt the user prompt
     * @param model  the model used
     * @param params additional parameters affecting the response
     * @return a stable cache key
     */
    String computeKey(String prompt, String model, String params);

    /**
     * Invalidate a cache entry.
     */
    void evict(String cacheKey);

    /**
     * Invalidate all cache entries.
     */
    void evictAll();
}
