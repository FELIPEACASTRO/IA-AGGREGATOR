package com.ia.aggregator.presentation.analytics;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AnalyticsReportSummaryResponse(
        UUID id,
        String source,
        Instant generatedAt,
        Instant receivedAt,
        int totalEvents,
        Map<String, Integer> counters
) {
}
