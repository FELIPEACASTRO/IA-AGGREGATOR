package com.ia.aggregator.infrastructure.knowledge;

import com.ia.aggregator.application.knowledge.port.out.VectorStorePort;
import com.ia.aggregator.domain.knowledge.DocumentChunk;
import com.ia.aggregator.domain.knowledge.RetrievalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * pgvector-backed vector store for knowledge base embeddings.
 *
 * <p>Uses PostgreSQL with the pgvector extension for similarity search.
 * Collections are stored in the content schema.
 */
@Component
public class PgVectorStoreAdapter implements VectorStorePort {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreAdapter.class);

    private final JdbcTemplate jdbcTemplate;

    public PgVectorStoreAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsertChunks(UUID collectionId, List<DocumentChunk> chunks) {
        String sql = """
                INSERT INTO content.document_chunks (id, document_id, collection_id, chunk_index, content, token_count, embedding, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?::vector, ?::jsonb)
                ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, embedding = EXCLUDED.embedding
                """;

        for (DocumentChunk chunk : chunks) {
            try {
                jdbcTemplate.update(sql,
                        chunk.id(), chunk.documentId(), collectionId, chunk.index(),
                        chunk.content(), chunk.tokenCount(),
                        arrayToVectorString(chunk.embedding()),
                        "{}");
            } catch (Exception e) {
                log.warn("Failed to upsert chunk {}: {}", chunk.id(), e.getMessage());
            }
        }
        log.info("Upserted {} chunks to collection {}", chunks.size(), collectionId);
    }

    @Override
    public List<RetrievalResult> search(UUID collectionId, float[] queryEmbedding, int topK, double scoreThreshold) {
        String sql = """
                SELECT c.id, c.document_id, c.content,
                       1 - (c.embedding <=> ?::vector) as score
                FROM content.document_chunks c
                WHERE c.collection_id = ?
                  AND 1 - (c.embedding <=> ?::vector) > ?
                ORDER BY c.embedding <=> ?::vector
                LIMIT ?
                """;

        String vectorStr = arrayToVectorString(queryEmbedding);
        try {
            return jdbcTemplate.query(sql,
                    (rs, rowNum) -> new RetrievalResult(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("document_id")),
                            null, // title resolved separately
                            rs.getString("content"),
                            rs.getDouble("score"),
                            null
                    ),
                    vectorStr, collectionId, vectorStr, scoreThreshold, vectorStr, topK);
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteByDocument(UUID documentId) {
        jdbcTemplate.update("DELETE FROM content.document_chunks WHERE document_id = ?", documentId);
    }

    @Override
    public void createCollection(UUID collectionId, int dimensions) {
        log.info("Collection {} registered (dimensions={})", collectionId, dimensions);
        // pgvector uses a shared table; collections are differentiated by collection_id
    }

    @Override
    public void deleteCollection(UUID collectionId) {
        jdbcTemplate.update("DELETE FROM content.document_chunks WHERE collection_id = ?", collectionId);
    }

    private String arrayToVectorString(float[] vector) {
        if (vector == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
