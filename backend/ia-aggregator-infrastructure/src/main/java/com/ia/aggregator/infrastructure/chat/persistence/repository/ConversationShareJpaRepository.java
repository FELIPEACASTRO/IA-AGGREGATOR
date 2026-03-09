package com.ia.aggregator.infrastructure.chat.persistence.repository;

import com.ia.aggregator.infrastructure.chat.persistence.entity.ConversationShareJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationShareJpaRepository extends JpaRepository<ConversationShareJpaEntity, UUID> {

    Optional<ConversationShareJpaEntity> findByShareToken(String token);
}
