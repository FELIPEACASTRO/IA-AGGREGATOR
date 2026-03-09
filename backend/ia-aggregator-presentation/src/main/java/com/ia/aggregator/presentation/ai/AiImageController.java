package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.ImageEditRequest;
import com.ia.aggregator.application.ai.dto.ImageGenRequest;
import com.ia.aggregator.application.ai.dto.ImageGenResult;
import com.ia.aggregator.application.ai.port.in.ImageEditUseCase;
import com.ia.aggregator.application.ai.port.in.ImageGenerationUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AI image generation and editing.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/ai/images/generate — generate images from text prompts</li>
 *   <li>POST /api/v1/ai/images/edit — edit existing images with text instructions</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ai/images")
public class AiImageController {

    private final ImageGenerationUseCase imageGenerationUseCase;
    private final ImageEditUseCase imageEditUseCase;

    public AiImageController(ImageGenerationUseCase imageGenerationUseCase,
                             ImageEditUseCase imageEditUseCase) {
        this.imageGenerationUseCase = imageGenerationUseCase;
        this.imageEditUseCase = imageEditUseCase;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ImageGenResult>> generate(@Valid @RequestBody ImageGenRequest request) {
        ImageGenResult result = imageGenerationUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/edit")
    public ResponseEntity<ApiResponse<ImageGenResult>> edit(@Valid @RequestBody ImageEditRequest request) {
        ImageGenResult result = imageEditUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
