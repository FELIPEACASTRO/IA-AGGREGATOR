package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;
import com.ia.aggregator.application.auth.port.in.LogoutUseCase;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final TokenProvider tokenProvider;

    public LogoutUseCaseImpl(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public void execute(UUID authenticatedUserId, RefreshTokenCommand command) {
        if (!tokenProvider.isRefreshTokenValid(command.refreshToken())) {
            throw new BusinessException(ErrorCode.AUTH_004, "Invalid or expired refresh token");
        }

        UUID refreshTokenUserId = tokenProvider.extractUserId(command.refreshToken());
        if (!authenticatedUserId.equals(refreshTokenUserId)) {
            throw new BusinessException(ErrorCode.AUTH_005, "Refresh token does not belong to authenticated user");
        }

        tokenProvider.revokeRefreshToken(command.refreshToken());
    }
}
