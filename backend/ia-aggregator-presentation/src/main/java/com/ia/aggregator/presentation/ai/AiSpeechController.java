package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.SpeechToTextRequest;
import com.ia.aggregator.application.ai.dto.SynthesisResult;
import com.ia.aggregator.application.ai.dto.TextToSpeechRequest;
import com.ia.aggregator.application.ai.dto.TranscriptionResult;
import com.ia.aggregator.application.ai.port.in.SpeechToTextUseCase;
import com.ia.aggregator.application.ai.port.in.TextToSpeechUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for speech operations (transcription and synthesis).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/ai/speech/transcribe — speech-to-text</li>
 *   <li>POST /api/v1/ai/speech/synthesize — text-to-speech</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ai/speech")
public class AiSpeechController {

    private final SpeechToTextUseCase speechToTextUseCase;
    private final TextToSpeechUseCase textToSpeechUseCase;

    public AiSpeechController(SpeechToTextUseCase speechToTextUseCase,
                              TextToSpeechUseCase textToSpeechUseCase) {
        this.speechToTextUseCase = speechToTextUseCase;
        this.textToSpeechUseCase = textToSpeechUseCase;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<ApiResponse<TranscriptionResult>> transcribe(
            @Valid @RequestBody SpeechToTextRequest request) {
        TranscriptionResult result = speechToTextUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/synthesize")
    public ResponseEntity<ApiResponse<SynthesisResult>> synthesize(
            @Valid @RequestBody TextToSpeechRequest request) {
        SynthesisResult result = textToSpeechUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
