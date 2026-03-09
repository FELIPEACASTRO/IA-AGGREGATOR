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
    private final UUID orgId;
    private final String email;

    public AuthenticatedUser(UUID userId, String email, String password,
                              Collection<? extends GrantedAuthority> authorities,
                              boolean accountNonLocked, boolean enabled) {
        this(userId, null, email, password, authorities, accountNonLocked, enabled);
    }

    public AuthenticatedUser(UUID userId, UUID orgId, String email, String password,
                              Collection<? extends GrantedAuthority> authorities,
                              boolean accountNonLocked, boolean enabled) {
        super(email, password, enabled, true, true, accountNonLocked, authorities);
        this.userId = userId;
        this.orgId = orgId;
        this.email = email;
    }

    public UUID getUserId() { return userId; }
    public UUID getOrgId() { return orgId; }
    public String getEmail() { return email; }
}
