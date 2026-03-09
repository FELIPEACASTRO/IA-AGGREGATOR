package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.OcrRequest;
import com.ia.aggregator.application.ai.dto.OcrResult;
import com.ia.aggregator.application.ai.port.in.OcrUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for optical character recognition (OCR).
 *
 * <p>Endpoint: POST /api/v1/ai/ocr
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiOcrController {

    private final OcrUseCase ocrUseCase;

    public AiOcrController(OcrUseCase ocrUseCase) {
        this.ocrUseCase = ocrUseCase;
    }

    @PostMapping("/ocr")
    public ResponseEntity<ApiResponse<OcrResult>> ocr(@Valid @RequestBody OcrRequest request) {
        OcrResult result = ocrUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
