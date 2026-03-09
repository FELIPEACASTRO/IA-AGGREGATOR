package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.GroundedChatRequest;
import com.ia.aggregator.application.ai.dto.GroundedChatResult;
import com.ia.aggregator.application.ai.port.in.GroundedChatUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for web-grounded chat.
 *
 * <p>Endpoint: POST /api/v1/ai/grounded-chat
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiGroundedChatController {

    private final GroundedChatUseCase groundedChatUseCase;

    public AiGroundedChatController(GroundedChatUseCase groundedChatUseCase) {
        this.groundedChatUseCase = groundedChatUseCase;
    }

    @PostMapping("/grounded-chat")
    public ResponseEntity<ApiResponse<GroundedChatResult>> groundedChat(
            @Valid @RequestBody GroundedChatRequest request) {
        GroundedChatResult result = groundedChatUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
