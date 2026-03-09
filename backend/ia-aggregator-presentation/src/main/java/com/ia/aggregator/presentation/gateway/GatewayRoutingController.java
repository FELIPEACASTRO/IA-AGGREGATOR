package com.ia.aggregator.presentation.gateway;

import com.ia.aggregator.application.gateway.port.in.RoutingUseCase;
import com.ia.aggregator.application.gateway.port.out.SemanticCachePort;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.domain.ai.RoutingStrategy;
import com.ia.aggregator.domain.gateway.RoutingContext;
import com.ia.aggregator.domain.gateway.RoutingDecision;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Gateway routing administration and inspection endpoints.
 */
@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayRoutingController {

    private final RoutingUseCase routingUseCase;
    private final SemanticCachePort semanticCache;

    public GatewayRoutingController(RoutingUseCase routingUseCase,
                                     SemanticCachePort semanticCache) {
        this.routingUseCase = routingUseCase;
        this.semanticCache = semanticCache;
    }

    /**
     * Inspect routing decision for a given capability and model.
     * Useful for debugging and admin visibility.
     */
    @GetMapping("/routing/inspect")
    public ResponseEntity<RoutingDecision> inspectRouting(
            @RequestParam String capability,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String strategy) {

        RoutingContext context = new RoutingContext(
                UUID.randomUUID(),
                Capability.valueOf(capability.toUpperCase()),
                model,
                strategy != null ? RoutingStrategy.valueOf(strategy.toUpperCase()) : null,
                null, null, null, Map.of()
        );

        return ResponseEntity.ok(routingUseCase.route(context));
    }

    /**
     * Clear the semantic cache.
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        semanticCache.evictAll();
        return ResponseEntity.ok(Map.of("status", "cache_cleared"));
    }
}
