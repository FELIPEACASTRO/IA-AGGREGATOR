package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionRevocationFlowTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LogoutUseCaseImpl logoutUseCase;

    @InjectMocks
    private RefreshTokenUseCaseImpl refreshTokenUseCase;

    @Test
    void refreshWithRevokedToken_shouldFailAfterLogout() {
        UUID userId = UUID.randomUUID();
        String revokedRefreshToken = "revoked-refresh-token";
        RefreshTokenCommand command = new RefreshTokenCommand(revokedRefreshToken);

        when(tokenProvider.isRefreshTokenValid(revokedRefreshToken))
                .thenReturn(true)   // logout validation
                .thenReturn(false); // refresh after logout
        when(tokenProvider.extractUserId(revokedRefreshToken)).thenReturn(userId);

        logoutUseCase.execute(userId, command);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenUseCase.execute(command)
        );

        assertEquals(ErrorCode.AUTH_004, exception.getErrorCode());
        verify(tokenProvider).revokeRefreshToken(revokedRefreshToken);
        verifyNoInteractions(userRepository);
    }
}
