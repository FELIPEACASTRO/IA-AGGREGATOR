package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.RerankRequest;
import com.ia.aggregator.application.ai.dto.RerankResult;
import com.ia.aggregator.application.ai.port.in.RerankUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for document re-ranking.
 *
 * <p>Endpoint: POST /api/v1/ai/rerank
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiRerankController {

    private final RerankUseCase rerankUseCase;

    public AiRerankController(RerankUseCase rerankUseCase) {
        this.rerankUseCase = rerankUseCase;
    }

    @PostMapping("/rerank")
    public ResponseEntity<ApiResponse<RerankResult>> rerank(@Valid @RequestBody RerankRequest request) {
        RerankResult result = rerankUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
