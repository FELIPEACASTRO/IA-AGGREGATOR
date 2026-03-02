package com.ia.aggregator.infrastructure.auth.security;

import com.ia.aggregator.domain.auth.vo.AuthProvider;
import com.ia.aggregator.domain.auth.vo.UserRole;
import com.ia.aggregator.domain.auth.vo.UserStatus;
import com.ia.aggregator.infrastructure.auth.persistence.entity.UserJpaEntity;
import com.ia.aggregator.infrastructure.auth.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    @Test
    void loadUserByUsername_activeUser_shouldBeEnabled() {
        UserJpaEntity user = buildUser(UserStatus.ACTIVE);
        when(userJpaRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("a@b.com");

        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_suspendedUser_shouldBeLocked() {
        UserJpaEntity user = buildUser(UserStatus.SUSPENDED);
        when(userJpaRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("a@b.com");

        assertFalse(details.isAccountNonLocked());
        assertFalse(details.isEnabled());
    }

    @Test
    void loadUserByUsername_pendingVerificationUser_shouldBeEnabled() {
        UserJpaEntity user = buildUser(UserStatus.PENDING_VERIFICATION);
        when(userJpaRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("a@b.com");

        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
    }

    @Test
    void loadUserByUsername_inactiveUser_shouldBeDisabledAndUnlocked() {
        UserJpaEntity user = buildUser(UserStatus.INACTIVE);
        when(userJpaRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("a@b.com");

        assertFalse(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
    }

    @Test
    void loadUserByUsername_notFound_shouldThrow() {
        when(userJpaRepository.findByEmailAndDeletedAtIsNull("x@y.com"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("x@y.com"));
    }

    @Test
    void loadUserById_shouldReturnUserDetails() {
        UUID id = UUID.randomUUID();
        UserJpaEntity user = buildUser(UserStatus.ACTIVE);
        user.setId(id);
        when(userJpaRepository.findByIdAndDeletedAtIsNull(id))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserById(id);

        assertTrue(details.isEnabled());
        assertEquals("a@b.com", details.getUsername());
    }

    @Test
    void loadUserById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(userJpaRepository.findByIdAndDeletedAtIsNull(id))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserById(id));
    }

    @Test
    void loadUserByUsername_nullPassword_shouldUseEmptyString() {
        UserJpaEntity user = buildUser(UserStatus.ACTIVE);
        user.setPasswordHash(null);
        when(userJpaRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("a@b.com");

        assertEquals("", details.getPassword());
    }

    private UserJpaEntity buildUser(UserStatus status) {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("a@b.com");
        user.setPasswordHash("hash");
        user.setFullName("Test");
        user.setRole(UserRole.USER);
        user.setStatus(status);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}
