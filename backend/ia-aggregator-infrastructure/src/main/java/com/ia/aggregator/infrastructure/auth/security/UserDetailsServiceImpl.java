package com.ia.aggregator.infrastructure.auth.security;

import com.ia.aggregator.infrastructure.auth.persistence.entity.UserJpaEntity;
import com.ia.aggregator.infrastructure.auth.persistence.repository.UserJpaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserJpaRepository userJpaRepository;

    public UserDetailsServiceImpl(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserJpaEntity user = userJpaRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return buildUserDetails(user);
    }

    public UserDetails loadUserById(UUID userId) {
        UserJpaEntity user = userJpaRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(UserJpaEntity user) {
        boolean isLocked = "SUSPENDED".equals(user.getStatus());
        boolean isEnabled = "ACTIVE".equals(user.getStatus())
                || "PENDING_VERIFICATION".equals(user.getStatus());

        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                !isLocked,
                isEnabled
        );
    }
}
