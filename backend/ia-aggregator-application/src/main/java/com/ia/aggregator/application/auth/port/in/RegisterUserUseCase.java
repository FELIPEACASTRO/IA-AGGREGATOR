package com.ia.aggregator.application.auth.port.in;

import com.ia.aggregator.application.auth.dto.RegisterUserCommand;
import com.ia.aggregator.application.auth.dto.UserResponse;

/**
 * Use case port for user registration.
 */
public interface RegisterUserUseCase {

    UserResponse execute(RegisterUserCommand command);
}
