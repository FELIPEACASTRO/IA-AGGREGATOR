package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.ResponsesRequest;
import com.ia.aggregator.application.ai.dto.ResponsesResult;
import com.ia.aggregator.application.ai.port.in.ResponsesUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for structured responses with tool-use capabilities.
 *
 * <p>Endpoint: POST /api/v1/ai/responses
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiResponsesController {

    private final ResponsesUseCase responsesUseCase;

    public AiResponsesController(ResponsesUseCase responsesUseCase) {
        this.responsesUseCase = responsesUseCase;
    }

    @PostMapping("/responses")
    public ResponseEntity<ApiResponse<ResponsesResult>> responses(
            @Valid @RequestBody ResponsesRequest request) {
        ResponsesResult result = responsesUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
