package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.AiStreamEvent;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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
    public SseEmitter stream(@Valid @RequestBody ChatCommand command) {
        if (!command.streamRequested()) {
            throw new BusinessException(ErrorCode.AI_010, "Ative stream=true para usar o endpoint de streaming");
        }

        SseEmitter emitter = new SseEmitter(95_000L);
        CompletableFuture.runAsync(() -> streamResponse(command, emitter));
        return emitter;
    }

    private void streamResponse(ChatCommand command, SseEmitter emitter) {
        try {
            ChatResponse response = chatUseCase.execute(command);
            sendEvent(emitter, "delta", AiStreamEvent.delta(
                    response.providerUsed(),
                    response.modelUsed(),
                    response.requestId(),
                    response.content()
            ));
            sendEvent(emitter, "complete", AiStreamEvent.complete(
                    response.providerUsed(),
                    response.modelUsed(),
                    response.requestId(),
                    response.usage(),
                    response.estimatedCost(),
                    response.finishReason()
            ));
            emitter.complete();
        } catch (Exception exception) {
            try {
                sendEvent(emitter, "error", AiStreamEvent.error(
                        null,
                        command.preferredModel(),
                        null,
                        exception.getMessage()
                ));
                emitter.complete();
            } catch (IOException ioException) {
                emitter.completeWithError(exception);
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, AiStreamEvent payload) throws IOException {
        emitter.send(
                SseEmitter.event()
                        .name(eventName)
                        .data(payload, MediaType.APPLICATION_JSON)
        );
    }
}
