package com.ia.aggregator.infrastructure.knowledge.persistence.repository;

import com.ia.aggregator.infrastructure.knowledge.persistence.entity.KnowledgeDocumentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeDocumentJpaRepository extends JpaRepository<KnowledgeDocumentJpaEntity, UUID> {

    List<KnowledgeDocumentJpaEntity> findByCollectionId(UUID collectionId);

    List<KnowledgeDocumentJpaEntity> findByOrgId(UUID orgId);
}
