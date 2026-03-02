package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private RefreshTokenUseCaseImpl useCase;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.reconstitute(
                userId, null, "user@test.com", "hash",
                "Name", null, UserRole.USER, UserStatus.ACTIVE,
                AuthProvider.LOCAL, null, "pt-BR", "UTC",
                "REF", true, null, 0, null, Instant.now(), Instant.now()
        );
    }

    @Test
    void execute_shouldReturnNewTokenPair() {
        String oldRefreshToken = "old-refresh-token";
        RefreshTokenCommand command = new RefreshTokenCommand(oldRefreshToken);

        when(tokenProvider.isRefreshTokenValid(oldRefreshToken)).thenReturn(true);
        when(tokenProvider.extractUserId(oldRefreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(tokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("new-access");
        when(tokenProvider.generateRefreshToken(userId)).thenReturn("new-refresh");

        TokenResponse result = useCase.execute(command);

        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
        assertEquals(900L, result.expiresIn());

        verify(tokenProvider).revokeRefreshToken(oldRefreshToken);
    }

    @Test
    void execute_shouldThrowWhenRefreshTokenInvalid() {
        RefreshTokenCommand command = new RefreshTokenCommand("invalid");

        when(tokenProvider.isRefreshTokenValid("invalid")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(command));

        assertEquals(ErrorCode.AUTH_004, ex.getErrorCode());
    }

    @Test
    void execute_shouldThrowWhenUserNotFound() {
        RefreshTokenCommand command = new RefreshTokenCommand("valid-token");

        when(tokenProvider.isRefreshTokenValid("valid-token")).thenReturn(true);
        when(tokenProvider.extractUserId("valid-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(command));

        assertEquals(ErrorCode.AUTH_004, ex.getErrorCode());
    }

    @Test
    void execute_shouldThrowWhenUserNotActive() {
        User inactiveUser = User.reconstitute(
                userId, null, "user@test.com", "hash",
                "Name", null, UserRole.USER, UserStatus.INACTIVE,
                AuthProvider.LOCAL, null, "pt-BR", "UTC",
                "REF", true, null, 0, null, Instant.now(), Instant.now()
        );
        RefreshTokenCommand command = new RefreshTokenCommand("valid-token");

        when(tokenProvider.isRefreshTokenValid("valid-token")).thenReturn(true);
        when(tokenProvider.extractUserId("valid-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(command));

        assertEquals(ErrorCode.AUTH_002, ex.getErrorCode());
    }
}
