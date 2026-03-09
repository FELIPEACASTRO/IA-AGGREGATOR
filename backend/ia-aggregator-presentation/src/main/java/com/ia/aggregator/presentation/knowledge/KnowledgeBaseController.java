package com.ia.aggregator.presentation.knowledge;

import com.ia.aggregator.application.knowledge.port.in.DocumentIngestionUseCase;
import com.ia.aggregator.application.knowledge.port.in.RetrievalUseCase;
import com.ia.aggregator.domain.knowledge.ChunkingStrategy;
import com.ia.aggregator.domain.knowledge.KnowledgeDocument;
import com.ia.aggregator.domain.knowledge.RetrievalResult;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Knowledge base endpoints for document management and RAG.
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeBaseController {

    private final DocumentIngestionUseCase ingestionUseCase;
    private final RetrievalUseCase retrievalUseCase;

    public KnowledgeBaseController(DocumentIngestionUseCase ingestionUseCase,
                                    RetrievalUseCase retrievalUseCase) {
        this.ingestionUseCase = ingestionUseCase;
        this.retrievalUseCase = retrievalUseCase;
    }

    @PostMapping("/documents")
    @RequiresPermission(Permission.AI_DOCUMENT_PARSE)
    public ResponseEntity<KnowledgeDocument> ingestDocument(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {
        UUID collectionId = UUID.fromString(body.getOrDefault("collectionId", UUID.randomUUID().toString()));
        String title = body.get("title");
        String content = body.get("content");
        String mimeType = body.getOrDefault("mimeType", "text/plain");
        ChunkingStrategy strategy = ChunkingStrategy.valueOf(
                body.getOrDefault("chunkingStrategy", "FIXED_SIZE").toUpperCase());

        KnowledgeDocument doc = ingestionUseCase.ingest(
                user.getOrgId(), collectionId, title, content, mimeType, strategy);
        return ResponseEntity.ok(doc);
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<KnowledgeDocument> getDocumentStatus(@PathVariable UUID documentId) {
        KnowledgeDocument doc = ingestionUseCase.getStatus(documentId);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc);
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable UUID documentId) {
        ingestionUseCase.delete(documentId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PostMapping("/search")
    public ResponseEntity<List<RetrievalResult>> search(@RequestBody Map<String, Object> body) {
        UUID collectionId = UUID.fromString((String) body.get("collectionId"));
        String query = (String) body.get("query");
        int topK = (int) body.getOrDefault("topK", 5);
        double threshold = ((Number) body.getOrDefault("scoreThreshold", 0.3)).doubleValue();

        return ResponseEntity.ok(retrievalUseCase.retrieve(collectionId, query, topK, threshold));
    }

    @PostMapping("/rag")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<RetrievalUseCase.RagResponse> ragChat(@RequestBody Map<String, Object> body) {
        UUID collectionId = UUID.fromString((String) body.get("collectionId"));
        String query = (String) body.get("query");
        String systemPrompt = (String) body.get("systemPrompt");
        int topK = (int) body.getOrDefault("topK", 5);

        return ResponseEntity.ok(retrievalUseCase.ragChat(collectionId, query, systemPrompt, topK));
    }
}
