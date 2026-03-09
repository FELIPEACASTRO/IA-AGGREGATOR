package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.ThreeDGenerationRequest;
import com.ia.aggregator.application.ai.dto.ThreeDGenerationResult;
import com.ia.aggregator.application.ai.port.in.ThreeDGenerationUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for 3D model generation.
 *
 * <p>Endpoint: POST /api/v1/ai/3d/generate
 */
@RestController
@RequestMapping("/api/v1/ai")
public class Ai3DGenerationController {

    private final ThreeDGenerationUseCase threeDGenerationUseCase;

    public Ai3DGenerationController(ThreeDGenerationUseCase threeDGenerationUseCase) {
        this.threeDGenerationUseCase = threeDGenerationUseCase;
    }

    @PostMapping("/3d/generate")
    public ResponseEntity<ApiResponse<ThreeDGenerationResult>> generate(@Valid @RequestBody ThreeDGenerationRequest request) {
        ThreeDGenerationResult result = threeDGenerationUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
