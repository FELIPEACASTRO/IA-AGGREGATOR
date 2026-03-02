package com.ia.aggregator.presentation.auth;

import com.ia.aggregator.application.auth.dto.LoginCommand;
import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;
import com.ia.aggregator.application.auth.dto.RegisterUserCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;
import com.ia.aggregator.application.auth.dto.UserResponse;
import com.ia.aggregator.application.auth.port.in.GetCurrentUserUseCase;
import com.ia.aggregator.application.auth.port.in.LoginUseCase;
import com.ia.aggregator.application.auth.port.in.RefreshTokenUseCase;
import com.ia.aggregator.application.auth.port.in.RegisterUserUseCase;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller for authentication endpoints.
 *
 * POST /api/v1/auth/register  — Register new user
 * POST /api/v1/auth/login     — Login with email/password
 * POST /api/v1/auth/refresh   — Refresh JWT tokens
 * GET  /api/v1/auth/me        — Get current authenticated user
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterUserCommand command) {
        TokenResponse token = registerUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(token, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginCommand command) {
        TokenResponse token = loginUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenCommand command) {
        TokenResponse token = refreshTokenUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.ok(token));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        UserResponse user = getCurrentUserUseCase.execute(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
