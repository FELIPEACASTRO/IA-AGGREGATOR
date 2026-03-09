package com.ia.aggregator.application.gateway.port.out;

/**
 * Port for accessing real-time provider performance metrics.
 *
 * <p>Used by the routing engine to make latency/cost-aware decisions.
 */
public interface ProviderMetricsPort {

    /**
     * Get the P50 latency in milliseconds for a provider+model.
     *
     * @param providerName provider name
     * @param model        model name
     * @return P50 latency in ms, or -1 if no data available
     */
    long getP50LatencyMs(String providerName, String model);

    /**
     * Get the estimated cost per 1K tokens for a provider+model.
     *
     * @param providerName provider name
     * @param model        model name
     * @return cost in USD per 1K tokens, or -1 if unknown
     */
    double getCostPer1kTokens(String providerName, String model);

    /**
     * Get the error rate (0.0-1.0) for a provider over the last window.
     *
     * @param providerName provider name
     * @return error rate, or 0.0 if no data
     */
    double getErrorRate(String providerName);

    /**
     * Get the quality score (0.0-1.0) for a model based on evaluations.
     *
     * @param model model name
     * @return quality score, or 0.5 if unknown
     */
    double getQualityScore(String model);

    /**
     * Record a completed request's latency for future routing decisions.
     */
    void recordLatency(String providerName, String model, long latencyMs);

    /**
     * Record a request's cost for budget tracking.
     */
    void recordCost(String providerName, String model, double costUsd);
}
