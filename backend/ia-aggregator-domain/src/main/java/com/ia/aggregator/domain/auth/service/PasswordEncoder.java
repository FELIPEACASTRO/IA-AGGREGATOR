package com.ia.aggregator.domain.auth.service;

/**
 * Port for password encoding/verification.
 * Implemented by infrastructure (BCrypt adapter).
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
