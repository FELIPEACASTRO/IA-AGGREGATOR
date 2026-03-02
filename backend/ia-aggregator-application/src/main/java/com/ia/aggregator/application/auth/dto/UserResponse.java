package com.ia.aggregator.application.auth.dto;

import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        String locale,
        String timezone,
        String referralCode,
        boolean emailVerified,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.getLocale(),
                user.getTimezone(),
                user.getReferralCode(),
                user.isEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
