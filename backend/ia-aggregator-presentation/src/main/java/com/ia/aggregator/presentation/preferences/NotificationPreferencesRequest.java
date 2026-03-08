package com.ia.aggregator.presentation.preferences;

public record NotificationPreferencesRequest(
        boolean emailEnabled,
        boolean pushEnabled,
        boolean billingAlerts,
        boolean usageAlerts,
        boolean securityAlerts,
        boolean productUpdates
) {
}
