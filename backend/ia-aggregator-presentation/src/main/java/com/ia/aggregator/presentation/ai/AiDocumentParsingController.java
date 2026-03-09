package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.DocumentParsingRequest;
import com.ia.aggregator.application.ai.dto.DocumentParsingResult;
import com.ia.aggregator.application.ai.port.in.DocumentParsingUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for document parsing.
 *
 * <p>Endpoint: POST /api/v1/ai/documents/parse
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiDocumentParsingController {

    private final DocumentParsingUseCase documentParsingUseCase;

    public AiDocumentParsingController(DocumentParsingUseCase documentParsingUseCase) {
        this.documentParsingUseCase = documentParsingUseCase;
    }

    @PostMapping("/documents/parse")
    public ResponseEntity<ApiResponse<DocumentParsingResult>> parse(@Valid @RequestBody DocumentParsingRequest request) {
        DocumentParsingResult result = documentParsingUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
