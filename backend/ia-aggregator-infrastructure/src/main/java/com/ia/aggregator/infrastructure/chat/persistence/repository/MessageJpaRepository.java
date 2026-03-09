package com.ia.aggregator.infrastructure.chat.persistence.repository;

import com.ia.aggregator.infrastructure.chat.persistence.entity.MessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageJpaRepository extends JpaRepository<MessageJpaEntity, UUID> {

    List<MessageJpaEntity> findByConversationIdOrderByCreatedAt(UUID conversationId);
}
