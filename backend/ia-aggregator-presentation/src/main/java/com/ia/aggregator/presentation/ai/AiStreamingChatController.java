package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.ChatCommand;
import com.ia.aggregator.application.ai.port.in.StreamingChatUseCase;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * SSE streaming endpoint for chat completions.
 *
 * <p>POST /api/v1/ai/chat/stream — Returns text/event-stream with token-by-token delivery.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiStreamingChatController {

    private final StreamingChatUseCase streamingChatUseCase;

    public AiStreamingChatController(StreamingChatUseCase streamingChatUseCase) {
        this.streamingChatUseCase = streamingChatUseCase;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@Valid @RequestBody ChatCommand command) {
        return streamingChatUseCase.executeStreaming(command)
                .timeout(Duration.ofSeconds(120))
                .concatWith(Flux.just("[DONE]"));
    }
}
