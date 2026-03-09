package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.TranslationRequest;
import com.ia.aggregator.application.ai.dto.TranslationResult;
import com.ia.aggregator.application.ai.port.in.TranslationUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for text translation.
 *
 * <p>Endpoint: POST /api/v1/ai/translate
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiTranslationController {

    private final TranslationUseCase translationUseCase;

    public AiTranslationController(TranslationUseCase translationUseCase) {
        this.translationUseCase = translationUseCase;
    }

    @PostMapping("/translate")
    public ResponseEntity<ApiResponse<TranslationResult>> translate(@Valid @RequestBody TranslationRequest request) {
        TranslationResult result = translationUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
