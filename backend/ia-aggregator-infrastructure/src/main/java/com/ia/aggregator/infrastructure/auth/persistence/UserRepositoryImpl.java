package com.ia.aggregator.infrastructure.auth.persistence;

import com.ia.aggregator.domain.auth.entity.User;
import com.ia.aggregator.domain.auth.repository.UserRepository;
import com.ia.aggregator.infrastructure.auth.persistence.entity.UserJpaEntity;
import com.ia.aggregator.infrastructure.auth.persistence.mapper.UserPersistenceMapper;
import com.ia.aggregator.infrastructure.auth.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the domain UserRepository port using JPA.
 */
@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryImpl(UserJpaRepository jpaRepository,
                               UserPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity jpaEntity;

        // Load-then-merge: preserve JPA-only fields (displayName, phone, metadata, etc.)
        var existing = jpaRepository.findById(user.getId());
        if (existing.isPresent()) {
            jpaEntity = existing.get();
            mapper.updateJpaEntity(jpaEntity, user);
        } else {
            jpaEntity = mapper.toJpaEntity(user);
        }

        UserJpaEntity saved = jpaRepository.save(jpaEntity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id)
                .map(mapper::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmailAndDeletedAtIsNull(email.toLowerCase().trim())
                .map(mapper::toDomainEntity);
    }

    @Override
    public Optional<User> findByReferralCode(String referralCode) {
        return jpaRepository.findByReferralCodeAndDeletedAtIsNull(referralCode)
                .map(mapper::toDomainEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmailAndDeletedAtIsNull(email.toLowerCase().trim());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setDeletedAt(Instant.now());
            jpaRepository.save(entity);
        });
    }
}
