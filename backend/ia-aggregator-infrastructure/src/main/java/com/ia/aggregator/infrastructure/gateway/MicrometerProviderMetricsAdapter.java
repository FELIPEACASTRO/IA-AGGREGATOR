package com.ia.aggregator.infrastructure.gateway;

import com.ia.aggregator.application.gateway.port.out.ProviderMetricsPort;
import com.ia.aggregator.infrastructure.ai.persistence.entity.AiModelJpaEntity;
import com.ia.aggregator.infrastructure.ai.persistence.entity.ProviderMetricJpaEntity;
import com.ia.aggregator.infrastructure.ai.persistence.repository.AiModelJpaRepository;
import com.ia.aggregator.infrastructure.ai.persistence.repository.ProviderMetricJpaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.YearMonth;
import java.util.concurrent.TimeUnit;

/**
 * DB + Micrometer-backed provider metrics for routing decisions.
 *
 * <p>Cost and quality data comes from the database (ai_gateway.models and ai_gateway.provider_metrics).
 * Latency data uses Micrometer timers for real-time accuracy.
 */
@Component
public class MicrometerProviderMetricsAdapter implements ProviderMetricsPort {

    private final MeterRegistry registry;
    private final AiModelJpaRepository modelRepository;
    private final ProviderMetricJpaRepository metricRepository;

    public MicrometerProviderMetricsAdapter(MeterRegistry registry,
                                             AiModelJpaRepository modelRepository,
                                             ProviderMetricJpaRepository metricRepository) {
        this.registry = registry;
        this.modelRepository = modelRepository;
        this.metricRepository = metricRepository;
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
        return modelRepository.findByModelId(model)
                .map(m -> {
                    Double input = m.getInputCostPer1k();
                    Double output = m.getOutputCostPer1k();
                    double inputCost = input != null ? input : 0.0;
                    double outputCost = output != null ? output : 0.0;
                    return (inputCost + outputCost) / 2.0;
                })
                .orElse(-1.0);
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
        return modelRepository.findByModelId(model)
                .map(m -> {
                    String period = YearMonth.now().toString();
                    return metricRepository.findByProviderIdAndModelIdAndPeriod(
                                    m.getProviderId(), m.getModelId(), period)
                            .map(ProviderMetricJpaEntity::getQualityScore)
                            .orElseGet(() -> resolveDefaultQuality(m));
                })
                .orElse(0.5);
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
    }

    private double resolveDefaultQuality(AiModelJpaEntity model) {
        String tier = model.getTier();
        if (tier == null) return 0.5;
        return switch (tier.toLowerCase()) {
            case "powerful" -> 0.9;
            case "balanced" -> 0.7;
            case "fast" -> 0.5;
            default -> 0.5;
        };
    }
}
