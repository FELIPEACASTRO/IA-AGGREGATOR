package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.AiHealthStatus;
import com.ia.aggregator.application.ai.service.AiProviderHealthService;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/providers")
public class AiProviderHealthController {

    private final AiProviderHealthService aiProviderHealthService;

    public AiProviderHealthController(AiProviderHealthService aiProviderHealthService) {
        this.aiProviderHealthService = aiProviderHealthService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<AiHealthStatus>>>> listProviders() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("providers", aiProviderHealthService.listProviders())));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, List<AiHealthStatus>>>> healthOverview() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("providers", aiProviderHealthService.listProviders())));
    }

    @GetMapping("/{providerId}/health")
    public ResponseEntity<ApiResponse<AiHealthStatus>> providerHealth(@PathVariable String providerId) {
        return ResponseEntity.ok(ApiResponse.ok(aiProviderHealthService.getProvider(providerId)));
    }
}
