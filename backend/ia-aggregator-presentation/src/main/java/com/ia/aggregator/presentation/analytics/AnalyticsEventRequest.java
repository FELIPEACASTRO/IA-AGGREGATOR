package com.ia.aggregator.presentation.analytics;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record AnalyticsEventRequest(
        @NotBlank(message = "event is required")
        String event,

        @NotBlank(message = "timestamp is required")
        String timestamp,

        Map<String, Object> metadata
) {
}
