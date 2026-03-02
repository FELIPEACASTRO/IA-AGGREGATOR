package com.ia.aggregator.application.auth.port.out;

import java.util.UUID;

/**
 * Port for JWT token generation and validation.
 * Implemented by infrastructure (JwtTokenProvider).
 */
public interface TokenProvider {

    String generateAccessToken(UUID userId, String email, String role);

    String generateRefreshToken(UUID userId);

    UUID extractUserId(String token);

    boolean validateToken(String token);

    void revokeRefreshToken(String refreshToken);

    boolean isRefreshTokenValid(String refreshToken);
}
