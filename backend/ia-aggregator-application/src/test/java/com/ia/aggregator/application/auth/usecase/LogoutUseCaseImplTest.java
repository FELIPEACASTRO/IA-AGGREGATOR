package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseImplTest {

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private LogoutUseCaseImpl useCase;

    @Test
    void execute_shouldRevokeRefreshTokenForAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        String refreshToken = "valid-refresh-token";
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        when(tokenProvider.isRefreshTokenValid(refreshToken)).thenReturn(true);
        when(tokenProvider.extractUserId(refreshToken)).thenReturn(userId);

        useCase.execute(userId, command);

        verify(tokenProvider).revokeRefreshToken(refreshToken);
    }

    @Test
    void execute_shouldThrowWhenRefreshTokenInvalid() {
        UUID userId = UUID.randomUUID();
        String refreshToken = "invalid-refresh-token";
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        when(tokenProvider.isRefreshTokenValid(refreshToken)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(userId, command));

        assertEquals(ErrorCode.AUTH_004, ex.getErrorCode());
        verify(tokenProvider, never()).extractUserId(refreshToken);
        verify(tokenProvider, never()).revokeRefreshToken(refreshToken);
    }

    @Test
    void execute_shouldThrowWhenRefreshTokenBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        String refreshToken = "valid-refresh-token";
        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        when(tokenProvider.isRefreshTokenValid(refreshToken)).thenReturn(true);
        when(tokenProvider.extractUserId(refreshToken)).thenReturn(anotherUserId);

        BusinessException ex = assertThrows(BusinessException.class, () -> useCase.execute(userId, command));

        assertEquals(ErrorCode.AUTH_005, ex.getErrorCode());
    }
}
