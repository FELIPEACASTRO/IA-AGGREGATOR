package com.ia.aggregator.application.auth.port.in;

import com.ia.aggregator.application.auth.dto.LoginCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;

/**
 * Use case port for user login.
 */
public interface LoginUseCase {

    TokenResponse execute(LoginCommand command);
}
