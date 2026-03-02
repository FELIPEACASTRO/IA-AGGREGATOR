package com.ia.aggregator.domain.auth.event;

import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.shared.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Emitted when a new user registers on the platform.
 */
public class UserRegisteredEvent extends BaseDomainEvent {

    private final UUID userId;
    private final String email;
    private final AuthProvider authProvider;

    public UserRegisteredEvent(UUID userId, String email, AuthProvider authProvider) {
        super();
        this.userId = userId;
        this.email = email;
        this.authProvider = authProvider;
    }

    @Override
    public String getEventType() {
        return "USER_REGISTERED";
    }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public AuthProvider getAuthProvider() { return authProvider; }
}
