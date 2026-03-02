package com.ia.aggregator.application.auth.port.in;

import com.ia.aggregator.application.auth.dto.RegisterUserCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;

/**
 * Use case port for user registration.
 * Returns tokens so the user is auto-logged-in after registration.
 */
public interface RegisterUserUseCase {

    TokenResponse execute(RegisterUserCommand command);
}
