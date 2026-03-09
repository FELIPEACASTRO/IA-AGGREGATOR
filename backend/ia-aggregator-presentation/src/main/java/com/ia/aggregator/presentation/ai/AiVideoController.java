package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.VideoGenRequest;
import com.ia.aggregator.application.ai.dto.VideoGenResult;
import com.ia.aggregator.application.ai.port.in.VideoGenerationUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AI video generation.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/ai/videos/generate — generate video from text or image prompts</li>
 * </ul>
 *
 * <p>Note: Video generation is typically asynchronous. The result contains a jobId
 * and status URL for polling completion. Status polling can be done via the
 * provider catalog endpoint or a dedicated async status endpoint.
 */
@RestController
@RequestMapping("/api/v1/ai/videos")
public class AiVideoController {

    private final VideoGenerationUseCase videoGenerationUseCase;

    public AiVideoController(VideoGenerationUseCase videoGenerationUseCase) {
        this.videoGenerationUseCase = videoGenerationUseCase;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<VideoGenResult>> generate(@Valid @RequestBody VideoGenRequest request) {
        VideoGenResult result = videoGenerationUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
