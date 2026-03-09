package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.AvatarVideoRequest;
import com.ia.aggregator.application.ai.dto.AvatarVideoResult;
import com.ia.aggregator.application.ai.port.in.AvatarVideoUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for avatar video generation.
 *
 * <p>Endpoint: POST /api/v1/ai/avatar-videos/generate
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiAvatarVideoController {

    private final AvatarVideoUseCase avatarVideoUseCase;

    public AiAvatarVideoController(AvatarVideoUseCase avatarVideoUseCase) {
        this.avatarVideoUseCase = avatarVideoUseCase;
    }

    @PostMapping("/avatar-videos/generate")
    public ResponseEntity<ApiResponse<AvatarVideoResult>> generate(@Valid @RequestBody AvatarVideoRequest request) {
        AvatarVideoResult result = avatarVideoUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
