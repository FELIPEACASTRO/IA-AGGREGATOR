package com.ia.aggregator.application.auth.port.in;

import com.ia.aggregator.application.auth.dto.UserResponse;

import java.util.UUID;

/**
 * Use case port for getting the current authenticated user.
 */
public interface GetCurrentUserUseCase {

    UserResponse execute(UUID userId);
}
