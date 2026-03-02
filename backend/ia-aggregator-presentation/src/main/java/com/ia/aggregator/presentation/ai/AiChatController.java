package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.ChatCommand;
import com.ia.aggregator.application.ai.dto.ChatResponse;
import com.ia.aggregator.application.ai.port.in.ChatUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final ChatUseCase chatUseCase;

    public AiChatController(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatCommand command) {
        ChatResponse response = chatUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
