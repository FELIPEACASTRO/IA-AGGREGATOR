package com.ia.aggregator.application.auth.usecase;

import com.ia.aggregator.application.auth.dto.RefreshTokenCommand;
import com.ia.aggregator.application.auth.dto.TokenResponse;
import com.ia.aggregator.application.auth.port.in.RefreshTokenUseCase;
import com.ia.aggregator.application.auth.port.out.TokenProvider;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900; // 15 min

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;

    public RefreshTokenUseCaseImpl(UserRepository userRepository,
                                    TokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public TokenResponse execute(RefreshTokenCommand command) {
        // Validate old refresh token
        if (!tokenProvider.isRefreshTokenValid(command.refreshToken())) {
            throw new BusinessException(ErrorCode.AUTH_004, "Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.extractUserId(command.refreshToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_004, "User not found"));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.AUTH_002, "Account is not active");
        }

        // Revoke old refresh token (rotation)
        tokenProvider.revokeRefreshToken(command.refreshToken());

        // Generate new token pair
        String accessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

        return TokenResponse.of(accessToken, newRefreshToken, ACCESS_TOKEN_EXPIRY_SECONDS);
    }
}
