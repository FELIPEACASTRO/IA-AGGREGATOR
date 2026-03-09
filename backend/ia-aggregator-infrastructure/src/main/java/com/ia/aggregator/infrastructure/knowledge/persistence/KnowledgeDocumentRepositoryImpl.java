package com.ia.aggregator.infrastructure.knowledge.persistence;

import com.ia.aggregator.application.knowledge.port.out.KnowledgeDocumentRepository;
import com.ia.aggregator.domain.knowledge.ChunkingStrategy;
import com.ia.aggregator.domain.knowledge.DocumentStatus;
import com.ia.aggregator.domain.knowledge.KnowledgeDocument;
import com.ia.aggregator.infrastructure.knowledge.persistence.entity.KnowledgeDocumentJpaEntity;
import com.ia.aggregator.infrastructure.knowledge.persistence.repository.KnowledgeDocumentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class KnowledgeDocumentRepositoryImpl implements KnowledgeDocumentRepository {

    private final KnowledgeDocumentJpaRepository jpa;

    public KnowledgeDocumentRepositoryImpl(KnowledgeDocumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(KnowledgeDocument doc) {
        KnowledgeDocumentJpaEntity entity = jpa.findById(doc.id())
                .orElseGet(KnowledgeDocumentJpaEntity::new);

        entity.setId(doc.id());
        entity.setOrgId(doc.orgId());
        entity.setCollectionId(doc.collectionId());
        entity.setTitle(doc.title());
        entity.setSourceUrl(doc.sourceUrl());
        entity.setMimeType(doc.mimeType());
        entity.setSizeBytes(doc.sizeBytes());
        entity.setStatus(doc.status().name().toLowerCase());
        entity.setChunkCount(doc.chunkCount());
        entity.setChunkingStrategy(doc.chunkingStrategy() != null ? doc.chunkingStrategy().name() : "FIXED_SIZE");
        entity.setEmbeddingModel(doc.embeddingModel());
        entity.setErrorMessage(doc.errorMessage());
        jpa.save(entity);
    }

    @Override
    public Optional<KnowledgeDocument> findById(UUID documentId) {
        return jpa.findById(documentId).map(this::toDomain);
    }

    @Override
    public void delete(UUID documentId) {
        jpa.deleteById(documentId);
    }

    private KnowledgeDocument toDomain(KnowledgeDocumentJpaEntity entity) {
        return new KnowledgeDocument(
                entity.getId(), entity.getOrgId(), entity.getCollectionId(),
                entity.getTitle(), entity.getSourceUrl(), entity.getMimeType(),
                entity.getSizeBytes(),
                DocumentStatus.valueOf(entity.getStatus().toUpperCase()),
                entity.getChunkCount(),
                ChunkingStrategy.valueOf(entity.getChunkingStrategy()),
                entity.getEmbeddingModel(),
                Map.of(),
                entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getErrorMessage()
        );
    }
}
