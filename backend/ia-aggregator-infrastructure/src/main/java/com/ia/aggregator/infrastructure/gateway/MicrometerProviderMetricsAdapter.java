package com.ia.aggregator.infrastructure.gateway;

import com.ia.aggregator.application.gateway.port.out.ProviderMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer-backed provider metrics for routing decisions.
 *
 * <p>Tracks latency, cost, and error rates per provider/model.
 */
@Component
public class MicrometerProviderMetricsAdapter implements ProviderMetricsPort {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Double> costCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> qualityScores = new ConcurrentHashMap<>();

    public MicrometerProviderMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public long getP50LatencyMs(String providerName, String model) {
        try {
            Timer timer = registry.find("ai.provider.latency")
                    .tag("provider", providerName)
                    .tag("model", model)
                    .timer();
            if (timer != null && timer.count() > 0) {
                HistogramSnapshot snapshot = timer.takeSnapshot();
                return (long) snapshot.percentileValues()[0].value(TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {
            // No metrics data yet
        }
        return -1;
    }

    @Override
    public double getCostPer1kTokens(String providerName, String model) {
        return costCache.getOrDefault(providerName + ":" + model, -1.0);
    }

    @Override
    public double getErrorRate(String providerName) {
        try {
            var errorCounter = registry.find("ai.provider.errors")
                    .tag("provider", providerName)
                    .counter();
            var totalCounter = registry.find("ai.provider.requests")
                    .tag("provider", providerName)
                    .counter();

            if (errorCounter != null && totalCounter != null && totalCounter.count() > 0) {
                return errorCounter.count() / totalCounter.count();
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }

    @Override
    public double getQualityScore(String model) {
        return qualityScores.getOrDefault(model, 0.5);
    }

    @Override
    public void recordLatency(String providerName, String model, long latencyMs) {
        Timer.builder("ai.provider.latency")
                .tag("provider", providerName)
                .tag("model", model)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(Duration.ofMillis(latencyMs));

        registry.counter("ai.provider.requests", "provider", providerName).increment();
    }

    @Override
    public void recordCost(String providerName, String model, double costUsd) {
        registry.counter("ai.provider.cost.usd",
                "provider", providerName, "model", model).increment(costUsd);
        costCache.put(providerName + ":" + model, costUsd);
    }
}
