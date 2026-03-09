package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.WebSearchRequest;
import com.ia.aggregator.application.ai.dto.WebSearchResult;
import com.ia.aggregator.application.ai.port.in.WebSearchUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for web search operations.
 *
 * <p>Endpoint: POST /api/v1/ai/search
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiSearchController {

    private final WebSearchUseCase webSearchUseCase;

    public AiSearchController(WebSearchUseCase webSearchUseCase) {
        this.webSearchUseCase = webSearchUseCase;
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<WebSearchResult>> search(@Valid @RequestBody WebSearchRequest request) {
        WebSearchResult result = webSearchUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
