package com.ia.aggregator.presentation.ai;

import com.ia.aggregator.application.ai.dto.ModelInfo;
import com.ia.aggregator.application.ai.dto.ProviderHealth;
import com.ia.aggregator.application.ai.port.out.MultiCapabilityProvider;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.routing.ProviderRegistry;
import com.ia.aggregator.presentation.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * REST controller for provider catalog, health status, and capability discovery.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/v1/ai/providers/catalog — full model/capability catalog</li>
 *   <li>GET /api/v1/ai/providers/capabilities — all available capabilities</li>
 *   <li>GET /api/v1/ai/providers/health — health of all providers</li>
 *   <li>GET /api/v1/ai/providers/{name}/health — health of a specific provider</li>
 *   <li>GET /api/v1/ai/providers/{name}/capabilities — capabilities of a specific provider</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ai/providers")
public class AiProviderCatalogController {

    private final ProviderRegistry providerRegistry;

    public AiProviderCatalogController(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<ModelInfo>>> catalog() {
        return ResponseEntity.ok(ApiResponse.ok(providerRegistry.catalog()));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<ApiResponse<Set<Capability>>> capabilities() {
        return ResponseEntity.ok(ApiResponse.ok(providerRegistry.allCapabilities()));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<List<ProviderHealth>>> healthAll() {
        return ResponseEntity.ok(ApiResponse.ok(providerRegistry.healthCheckAll()));
    }

    @GetMapping("/health/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> healthSummary() {
        return ResponseEntity.ok(ApiResponse.ok(providerRegistry.healthSummary()));
    }

    @GetMapping("/{name}/health")
    public ResponseEntity<ApiResponse<ProviderHealth>> healthByName(@PathVariable String name) {
        ProviderHealth health = providerRegistry.healthCheck(name);
        if ("UNKNOWN".equals(health.status())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(health));
    }

    @GetMapping("/{name}/capabilities")
    public ResponseEntity<ApiResponse<Set<Capability>>> capabilitiesByName(@PathVariable String name) {
        return providerRegistry.getProviderByName(name)
                .map(p -> ResponseEntity.ok(ApiResponse.ok(p.capabilities())))
                .orElse(ResponseEntity.notFound().build());
    }
}
