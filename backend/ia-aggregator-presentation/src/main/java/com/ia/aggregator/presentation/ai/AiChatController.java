package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.ChatCommand;
import com.ia.aggregator.application.ai.dto.ChatResponse;
import com.ia.aggregator.application.ai.port.in.ChatUseCase;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
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

    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<ApiResponse<Void>> stream(@Valid @RequestBody ChatCommand command) {
        if (!command.streamRequested()) {
            throw new BusinessException(ErrorCode.AI_010, "Ative stream=true para usar o endpoint de streaming");
        }

        throw new BusinessException(
                ErrorCode.AI_010,
                "Streaming real ainda nao foi habilitado para o provider selecionado na camada canônica"
        );
    }
}
