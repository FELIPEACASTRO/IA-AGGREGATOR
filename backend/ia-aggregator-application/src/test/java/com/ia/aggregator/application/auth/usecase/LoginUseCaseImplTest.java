package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.LoginCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.domain.auth.service.PasswordEncoder;
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
class LoginUseCaseImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private LoginUseCaseImpl useCase;

    private LoginCommand validCommand;
    private User activeUser;

    @BeforeEach
    void setUp() {
        validCommand = new LoginCommand("user@test.com", "password");
        activeUser = User.reconstitute(
                UUID.randomUUID(), null, "user@test.com", "hashedPwd",
                "John Doe", null, UserRole.USER, UserStatus.ACTIVE,
                AuthProvider.LOCAL, null, "pt-BR", "America/Sao_Paulo",
                "REF12345", true, null, 0, null, Instant.now(), Instant.now()
        );
    }

    @Test
    void execute_shouldReturnTokensForValidCredentials() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password", "hashedPwd")).thenReturn(true);
        when(tokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("access");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("refresh");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenResponse result = useCase.execute(validCommand);

        assertNotNull(result);
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        assertEquals(900L, result.expiresIn());
    }

    @Test
    void execute_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(validCommand));

        assertEquals(ErrorCode.AUTH_001, ex.getErrorCode());
    }

    @Test
    void execute_shouldThrowWhenAccountSuspended() {
        User suspended = User.reconstitute(
                UUID.randomUUID(), null, "user@test.com", "hash",
                "Name", null, UserRole.USER, UserStatus.SUSPENDED,
                AuthProvider.LOCAL, null, "pt-BR", "UTC",
                "REF", true, null, 0, null, Instant.now(), Instant.now()
        );
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(suspended));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(validCommand));

        assertEquals(ErrorCode.AUTH_002, ex.getErrorCode());
    }

    @Test
    void execute_shouldThrowWhenEmailNotVerified() {
        User pending = User.reconstitute(
                UUID.randomUUID(), null, "user@test.com", "hash",
                "Name", null, UserRole.USER, UserStatus.PENDING_VERIFICATION,
                AuthProvider.LOCAL, null, "pt-BR", "UTC",
                "REF", false, null, 0, null, Instant.now(), Instant.now()
        );
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(pending));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(validCommand));

        assertEquals(ErrorCode.AUTH_006, ex.getErrorCode());
    }

    @Test
    void execute_shouldThrowWhenPasswordDoesNotMatch() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password", "hashedPwd")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(validCommand));

        assertEquals(ErrorCode.AUTH_001, ex.getErrorCode());
        verify(userRepository).save(any(User.class)); // should save after recording failed login
    }

    @Test
    void execute_shouldThrowWhenOAuthUserTriesPasswordLogin() {
        User oauthUser = User.reconstitute(
                UUID.randomUUID(), null, "user@test.com", null,
                "OAuth User", null, UserRole.USER, UserStatus.ACTIVE,
                AuthProvider.GOOGLE, "google-123", "pt-BR", "UTC",
                "REF", true, null, 0, null, Instant.now(), Instant.now()
        );
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(oauthUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(validCommand));

        assertEquals(ErrorCode.AUTH_001, ex.getErrorCode());
    }

    @Test
    void execute_shouldThrowWhenAccountIsLocked() {
        User lockedUser = User.reconstitute(
                UUID.randomUUID(), null, "user@test.com", "hashedPwd",
                "Locked User", null, UserRole.USER, UserStatus.ACTIVE,
                AuthProvider.LOCAL, null, "pt-BR", "UTC",
                "REF", true, null, 5, Instant.now().plusSeconds(1800), Instant.now(), Instant.now()
        );
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(lockedUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(validCommand));

        assertEquals(ErrorCode.AUTH_002, ex.getErrorCode());
    }

    @Test
    void execute_shouldRecordLoginAndSave() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password", "hashedPwd")).thenReturn(true);
        when(tokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("at");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("rt");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(validCommand);

        verify(userRepository).save(any(User.class));
    }
}
