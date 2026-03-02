package com.ia.aggregator.infrastructure.auth.security;

import com.ia.aggregator.application.auth.port.out.TokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider using jjwt + Redis for refresh tokens.
 * Access token: 15 min, stateless.
 * Refresh token: 7 days, stored in Redis with rotation.
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final long ACCESS_TOKEN_EXPIRY_MS = Duration.ofMinutes(15).toMillis();
    private static final long REFRESH_TOKEN_EXPIRY_MS = Duration.ofDays(7).toMillis();
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    private final SecretKey signingKey;
    private final StringRedisTemplate redisTemplate;

    public JwtTokenProvider(@Value("${app.security.jwt.secret}") String jwtSecret,
                            StringRedisTemplate redisTemplate) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRY_MS);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        String tokenId = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_EXPIRY_MS);

        String token = Jwts.builder()
                .subject(userId.toString())
                .id(tokenId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();

        // Store in Redis with TTL
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + tokenId,
                userId.toString(),
                Duration.ofDays(7)
        );

        return token;
    }

    @Override
    public UUID extractUserId(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        try {
            Claims claims = parseToken(refreshToken);
            String tokenId = claims.getId();
            if (tokenId != null) {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + tokenId);
            }
        } catch (JwtException e) {
            log.debug("Failed to revoke refresh token: {}", e.getMessage());
        }
    }

    @Override
    public boolean isRefreshTokenValid(String refreshToken) {
        try {
            Claims claims = parseToken(refreshToken);
            String tokenId = claims.getId();
            if (tokenId == null) return false;

            // Check if token exists in Redis (not revoked)
            return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + tokenId));
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
