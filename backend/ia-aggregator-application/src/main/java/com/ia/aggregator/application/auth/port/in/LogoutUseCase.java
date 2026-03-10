package com.ia.aggregator.application.auth.port.in;

import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;

import java.util.UUID;

public interface LogoutUseCase {

    void execute(UUID authenticatedUserId, RefreshTokenCommand command);
}
