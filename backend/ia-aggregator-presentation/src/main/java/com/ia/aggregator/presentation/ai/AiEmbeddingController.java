package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.EmbeddingRequest;
import com.ia.aggregator.application.ai.dto.EmbeddingResult;
import com.ia.aggregator.application.ai.port.in.EmbeddingUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for text embedding generation.
 *
 * <p>Endpoint: POST /api/v1/ai/embeddings
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiEmbeddingController {

    private final EmbeddingUseCase embeddingUseCase;

    public AiEmbeddingController(EmbeddingUseCase embeddingUseCase) {
        this.embeddingUseCase = embeddingUseCase;
    }

    @PostMapping("/embeddings")
    public ResponseEntity<ApiResponse<EmbeddingResult>> embed(@Valid @RequestBody EmbeddingRequest request) {
        EmbeddingResult result = embeddingUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
