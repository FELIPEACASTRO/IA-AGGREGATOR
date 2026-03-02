package com.ia.aggregator.application.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
