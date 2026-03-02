package com.ia.aggregator.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(
        @NotBlank String refreshToken
) {}
