package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.ThreatIntelRequest;
import com.ia.aggregator.application.ai.dto.ThreatIntelResult;
import com.ia.aggregator.application.ai.port.in.ThreatIntelUseCase;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for threat intelligence search.
 *
 * <p>Endpoint: POST /api/v1/ai/threat-intel
 *
 * <p>Access is controlled by the compliance gate. Requests are blocked
 * unless {@code app.ai.compliance.dark-web-enabled=true} is configured.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiThreatIntelController {

    private final ThreatIntelUseCase threatIntelUseCase;

    public AiThreatIntelController(ThreatIntelUseCase threatIntelUseCase) {
        this.threatIntelUseCase = threatIntelUseCase;
    }

    @PostMapping("/threat-intel")
    public ResponseEntity<ApiResponse<ThreatIntelResult>> search(
            @Valid @RequestBody ThreatIntelRequest request) {
        ThreatIntelResult result = threatIntelUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
