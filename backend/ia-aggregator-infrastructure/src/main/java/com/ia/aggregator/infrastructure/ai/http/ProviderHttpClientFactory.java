package com.ia.aggregator.infrastructure.ai.http;

import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Singleton factory that creates and caches {@link HttpClient} instances shared
 * across AI providers with equivalent timeout configurations.
 *
 * <p><b>Problem solved</b>: Without this factory, each of the 50+ providers instantiates
 * its own {@code HttpClient}, resulting in O(N=50) HttpClient instances — each with its
 * own internal thread pools and connection pools. This wastes memory and defeats
 * Virtual Threads executor sharing.
 *
 * <p><b>Solution</b>: Providers with the same timeout share a single {@code HttpClient}.
 * In practice, most providers cluster around 2–3 timeout values (30s, 60s, 120s),
 * so K = O(2–3) instances are created instead of O(50).
 *
 * <p><b>Thread safety</b>: {@link ConcurrentHashMap#computeIfAbsent} is atomic;
 * only one {@code HttpClient} is created per timeout bucket even under concurrent init.
 *
 * <p><b>Big O</b>: O(1) amortized per lookup via ConcurrentHashMap.
 * O(K) total HttpClient instances, K = distinct timeout values.
 */
@Component
public class ProviderHttpClientFactory {

    /**
     * Cache key is the connect-timeout in milliseconds.
     * Value is the shared {@link HttpClient} instance.
     */
    private final ConcurrentHashMap<Long, HttpClient> clientCache = new ConcurrentHashMap<>();

    /**
     * Returns an {@link HttpClient} configured with the given connect timeout,
     * backed by a virtual-thread-per-task executor (Java 21 Project Loom).
     *
     * <p>Clients with the same {@code timeoutMs} are reused — never create a new
     * client in calling code.
     *
     * @param timeoutMs connect timeout in milliseconds; must be positive
     * @return cached or newly created {@link HttpClient}
     */
    public HttpClient getOrCreate(long timeoutMs) {
        return clientCache.computeIfAbsent(timeoutMs, t ->
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(t))
                        .executor(Executors.newVirtualThreadPerTaskExecutor())
                        .build()
        );
    }
}
