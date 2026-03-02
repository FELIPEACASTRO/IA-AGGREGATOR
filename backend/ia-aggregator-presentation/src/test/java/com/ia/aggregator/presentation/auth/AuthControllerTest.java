package com.ia.aggregator.presentation.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.auth.dto.LoginCommand;
import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;
import com.ia.aggregator.application.auth.dto.RegisterUserCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;
import com.ia.aggregator.application.auth.dto.UserResponse;
import com.ia.aggregator.application.auth.port.in.GetCurrentUserUseCase;
import com.ia.aggregator.application.auth.port.in.LoginUseCase;
import com.ia.aggregator.application.auth.port.in.RefreshTokenUseCase;
import com.ia.aggregator.application.auth.port.in.RegisterUserUseCase;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.presentation.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RegisterUserUseCase registerUserUseCase;
    @Mock
    private LoginUseCase loginUseCase;
    @Mock
    private RefreshTokenUseCase refreshTokenUseCase;
    @Mock
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    // ── Register ─────────────────────────────────────────────────────────

    @Test
    void register_shouldReturn201WithTokenResponse() throws Exception {
        var command = new RegisterUserCommand("user@test.com", "Password123!", "John Doe", null);
        var tokenResponse = TokenResponse.of("access-token", "refresh-token", 900);

        when(registerUserUseCase.execute(any(RegisterUserCommand.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(registerUserUseCase).execute(any(RegisterUserCommand.class));
    }

    @Test
    void register_duplicateEmail_shouldReturn409() throws Exception {
        var command = new RegisterUserCommand("dup@test.com", "Password123!", "John Doe", null);

        when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.AUTH_003, "Email already in use"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_003"))
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void register_invalidBody_shouldReturn400() throws Exception {
        // email missing, password too short, fullName blank
        String invalidBody = """
                {"email": "", "password": "short", "fullName": ""}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(registerUserUseCase);
    }

    // ── Login ────────────────────────────────────────────────────────────

    @Test
    void login_shouldReturn200WithTokenResponse() throws Exception {
        var command = new LoginCommand("user@test.com", "Password123!");
        var tokenResponse = TokenResponse.of("access-token", "refresh-token", 900);

        when(loginUseCase.execute(any(LoginCommand.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        verify(loginUseCase).execute(any(LoginCommand.class));
    }

    @Test
    void login_invalidCredentials_shouldReturn401() throws Exception {
        var command = new LoginCommand("user@test.com", "wrongpass");

        when(loginUseCase.execute(any(LoginCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.AUTH_001));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));
    }

    // ── Refresh ──────────────────────────────────────────────────────────

    @Test
    void refresh_shouldReturn200WithNewTokens() throws Exception {
        var command = new RefreshTokenCommand("old-refresh-token");
        var tokenResponse = TokenResponse.of("new-access", "new-refresh", 900);

        when(refreshTokenUseCase.execute(any(RefreshTokenCommand.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_invalidToken_shouldReturn401() throws Exception {
        var command = new RefreshTokenCommand("expired-token");

        when(refreshTokenUseCase.execute(any(RefreshTokenCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.AUTH_004));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));
    }

    @Test
    void refresh_blankToken_shouldReturn400() throws Exception {
        String body = """
                {"refreshToken": ""}
                """;

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(refreshTokenUseCase);
    }

    // ── Get Current User ─────────────────────────────────────────────────

    @Test
    void me_shouldReturn200WithUserResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        var userResponse = new UserResponse(
                userId, "user@test.com", "John Doe", null,
                UserRole.USER, UserStatus.ACTIVE,
                "pt-BR", "UTC", "REF123",
                true, Instant.now(), Instant.now()
        );

        when(getCurrentUserUseCase.execute(userId)).thenReturn(userResponse);

        var principal = new AuthenticatedUser(
                userId, "user@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                true, true
        );

        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.fullName").value("John Doe"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(getCurrentUserUseCase).execute(userId);
    }
}
