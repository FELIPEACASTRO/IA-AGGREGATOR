package com.ia.aggregator.presentation.preferences;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserPreferencesRequest(
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Pattern(regexp = "^[a-z]{2}(?:-[A-Z]{2})?$", message = "locale must be in ll or ll-CC format") String locale,
        @NotBlank @Pattern(regexp = "^(light|dark|system)$") String theme,
        @NotBlank @Pattern(regexp = "^(default|sans|system|dyslexia)$") String chatFontMode
) {
}
