package com.ia.aggregator.application.auth.port.in;

import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;

/**
 * Use case port for refreshing JWT tokens.
 */
public interface RefreshTokenUseCase {

    TokenResponse execute(RefreshTokenCommand command);
}
