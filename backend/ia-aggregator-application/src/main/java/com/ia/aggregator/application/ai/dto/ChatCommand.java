package com.ia.aggregator.application.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatCommand(
        @NotBlank(message = "prompt is required")
        String prompt,
        String preferredModel
) {
}
