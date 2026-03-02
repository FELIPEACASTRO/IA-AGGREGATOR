package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.RegisterUserCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.application.auth.port.out.UserEventPublisher;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.domain.auth.service.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private UserEventPublisher eventPublisher;

    @InjectMocks
    private RegisterUserUseCaseImpl useCase;

    private RegisterUserCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new RegisterUserCommand("user@test.com", "StrongPass123!", "John Doe", null);
    }

    @Test
    void execute_shouldRegisterUserAndReturnTokens() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any(), eq("user@test.com"), eq("USER")))
                .thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        TokenResponse result = useCase.execute(validCommand);

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals(900L, result.expiresIn());
        assertEquals("Bearer", result.tokenType());

        verify(userRepository).save(any(User.class));
        verify(eventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void execute_shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(validCommand));

        assertEquals(ErrorCode.AUTH_003, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_shouldHashPasswordBeforeSaving() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any(), anyString(), anyString())).thenReturn("at");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("rt");

        useCase.execute(validCommand);

        verify(passwordEncoder).encode("StrongPass123!");
    }
}
