package com.ia.aggregator.infrastructure.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Custom UserDetails that carries the domain userId (UUID)
 * alongside the standard Spring Security fields.
 */
public class AuthenticatedUser extends User {

    private final UUID userId;

    public AuthenticatedUser(UUID userId, String email, String password,
                              Collection<? extends GrantedAuthority> authorities,
                              boolean accountNonLocked, boolean enabled) {
        super(email, password, enabled, true, true, accountNonLocked, authorities);
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
}
