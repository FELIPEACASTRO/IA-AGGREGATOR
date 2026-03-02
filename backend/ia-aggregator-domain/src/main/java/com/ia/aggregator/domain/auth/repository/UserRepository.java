package com.ia.aggregator.domain.auth.repository;

import com.ia.aggregator.domain.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for User aggregate.
 * Implemented by infrastructure layer.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByReferralCode(String referralCode);

    boolean existsByEmail(String email);

    void deleteById(UUID id);
}
