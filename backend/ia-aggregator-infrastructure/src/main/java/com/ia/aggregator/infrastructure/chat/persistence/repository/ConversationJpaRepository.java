package com.ia.aggregator.infrastructure.chat.persistence.repository;

import com.ia.aggregator.infrastructure.chat.persistence.entity.ConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    List<ConversationJpaEntity> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID userId);

    List<ConversationJpaEntity> findByOrgIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID orgId);

    Optional<ConversationJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
