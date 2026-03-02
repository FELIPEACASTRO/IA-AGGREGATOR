package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.UserResponse;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetCurrentUserUseCaseImpl useCase;

    @Test
    void execute_shouldReturnUserResponse() {
        UUID userId = UUID.randomUUID();
        User user = User.reconstitute(
                userId, null, "user@test.com", "hash",
                "John Doe", "https://avatar.url", UserRole.USER, UserStatus.ACTIVE,
                AuthProvider.LOCAL, null, "pt-BR", "UTC",
                "REF12345", true, null, 0, null, Instant.now(), Instant.now()
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = useCase.execute(userId);

        assertEquals(userId, result.id());
        assertEquals("user@test.com", result.email());
        assertEquals("John Doe", result.fullName());
        assertEquals("https://avatar.url", result.avatarUrl());
        assertEquals(UserRole.USER, result.role());
        assertEquals(UserStatus.ACTIVE, result.status());
    }

    @Test
    void execute_shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> useCase.execute(userId));

        assertEquals(ErrorCode.GEN_003, ex.getErrorCode());
    }
}
