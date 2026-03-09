package com.ia.aggregator.application.knowledge.usecase;

import com.ia.aggregator.application.knowledge.port.in.DocumentIngestionUseCase;
import com.ia.aggregator.application.knowledge.port.out.DocumentChunkerPort;
import com.ia.aggregator.application.knowledge.port.out.EmbeddingPort;
import com.ia.aggregator.application.knowledge.port.out.KnowledgeDocumentRepository;
import com.ia.aggregator.application.knowledge.port.out.VectorStorePort;
import com.ia.aggregator.domain.knowledge.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Document ingestion pipeline: parse → chunk → embed → store.
 */
@Service
public class DocumentIngestionUseCaseImpl implements DocumentIngestionUseCase {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionUseCaseImpl.class);

    private final DocumentChunkerPort chunker;
    private final EmbeddingPort embeddingPort;
    private final VectorStorePort vectorStore;
    private final KnowledgeDocumentRepository documentRepository;

    @Value("${app.knowledge.embedding-model:text-embedding-3-small}")
    private String defaultEmbeddingModel;
    @Value("${app.knowledge.chunk-size:512}")
    private int defaultChunkSize;
    @Value("${app.knowledge.chunk-overlap:50}")
    private int defaultChunkOverlap;

    public DocumentIngestionUseCaseImpl(DocumentChunkerPort chunker,
                                         EmbeddingPort embeddingPort,
                                         VectorStorePort vectorStore,
                                         KnowledgeDocumentRepository documentRepository) {
        this.chunker = chunker;
        this.embeddingPort = embeddingPort;
        this.vectorStore = vectorStore;
        this.documentRepository = documentRepository;
    }

    @Override
    public KnowledgeDocument ingest(UUID orgId, UUID collectionId, String title,
                                     String content, String mimeType, ChunkingStrategy strategy) {
        KnowledgeDocument doc = KnowledgeDocument.create(orgId, collectionId, title, mimeType,
                content.length(), strategy);
        documentRepository.save(doc);

        try {
            // Step 1: Chunk
            log.info("Chunking document: {} ({} bytes)", title, content.length());
            List<DocumentChunk> chunks = chunker.chunk(doc.id(), content, strategy,
                    defaultChunkSize, defaultChunkOverlap);

            // Step 2: Embed
            log.info("Embedding {} chunks for document: {}", chunks.size(), title);
            List<String> texts = chunks.stream().map(DocumentChunk::content).toList();
            List<float[]> embeddings = embeddingPort.embed(texts, defaultEmbeddingModel);

            // Merge embeddings into chunks
            List<DocumentChunk> embeddedChunks = new java.util.ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk c = chunks.get(i);
                embeddedChunks.add(new DocumentChunk(c.id(), c.documentId(), c.index(),
                        c.content(), c.tokenCount(), embeddings.get(i), c.metadata()));
            }

            // Step 3: Store in vector DB
            vectorStore.upsertChunks(collectionId, embeddedChunks);

            KnowledgeDocument indexed = new KnowledgeDocument(doc.id(), orgId, collectionId, title,
                    doc.sourceUrl(), mimeType, doc.sizeBytes(), DocumentStatus.INDEXED,
                    chunks.size(), strategy, defaultEmbeddingModel, doc.metadata(),
                    doc.createdAt(), java.time.Instant.now(), null);
            documentRepository.save(indexed);
            log.info("Document indexed: {} ({} chunks)", title, chunks.size());
            return indexed;

        } catch (Exception e) {
            log.error("Document ingestion failed: {}", e.getMessage());
            KnowledgeDocument failed = new KnowledgeDocument(doc.id(), orgId, collectionId, title,
                    doc.sourceUrl(), mimeType, doc.sizeBytes(), DocumentStatus.FAILED,
                    0, strategy, null, doc.metadata(),
                    doc.createdAt(), java.time.Instant.now(), e.getMessage());
            documentRepository.save(failed);
            return failed;
        }
    }

    @Override
    public KnowledgeDocument getStatus(UUID documentId) {
        return documentRepository.findById(documentId).orElse(null);
    }

    @Override
    public void delete(UUID documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId).orElse(null);
        if (doc != null) {
            documentRepository.delete(documentId);
            vectorStore.deleteByDocument(documentId);
        }
    }
}
