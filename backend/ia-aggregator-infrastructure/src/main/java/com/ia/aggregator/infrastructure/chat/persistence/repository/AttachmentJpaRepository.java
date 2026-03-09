package com.ia.aggregator.infrastructure.chat.persistence.repository;

import com.ia.aggregator.infrastructure.chat.persistence.entity.AttachmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentJpaRepository extends JpaRepository<AttachmentJpaEntity, UUID> {

    List<AttachmentJpaEntity> findByMessageId(UUID messageId);
}
