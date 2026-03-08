package com.ia.aggregator.presentation.preferences;

public record UserPreferencesResponse(
        String fullName,
        String locale,
        String theme,
        String chatFontMode
) {
}
