package com.ia.aggregator.presentation.marketplace;

import com.ia.aggregator.application.marketplace.port.in.CatalogUseCase;
import com.ia.aggregator.domain.marketplace.CatalogEntry;
import com.ia.aggregator.domain.marketplace.CatalogReview;
import com.ia.aggregator.infrastructure.auth.security.AuthenticatedUser;
import com.ia.aggregator.infrastructure.auth.security.RequiresPermission;
import com.ia.aggregator.domain.auth.vo.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Marketplace and catalog endpoints.
 */
@RestController
@RequestMapping("/api/v1/marketplace")
public class MarketplaceController {

    private final CatalogUseCase catalogUseCase;

    public MarketplaceController(CatalogUseCase catalogUseCase) {
        this.catalogUseCase = catalogUseCase;
    }

    @PostMapping("/catalog")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<CatalogEntry> publish(@RequestBody CatalogEntry entry) {
        return ResponseEntity.ok(catalogUseCase.publish(entry));
    }

    @GetMapping("/catalog/{id}")
    public ResponseEntity<CatalogEntry> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogUseCase.getById(id));
    }

    @GetMapping("/catalog/search")
    public ResponseEntity<List<CatalogEntry>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CatalogEntry.EntryType entryType = type != null ?
                CatalogEntry.EntryType.valueOf(type.toUpperCase()) : null;
        return ResponseEntity.ok(catalogUseCase.search(q, entryType, categories, page, size));
    }

    @GetMapping("/catalog/popular")
    public ResponseEntity<List<CatalogEntry>> popular(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogUseCase.listPopular(page, size));
    }

    @PostMapping("/catalog/{id}/install")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<Map<String, String>> install(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        catalogUseCase.install(id, user.getOrgId());
        return ResponseEntity.ok(Map.of("status", "installed"));
    }

    @PostMapping("/catalog/{id}/reviews")
    @RequiresPermission(Permission.CHAT_CREATE)
    public ResponseEntity<CatalogReview> addReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, Object> body) {
        int rating = ((Number) body.get("rating")).intValue();
        String comment = (String) body.get("comment");
        return ResponseEntity.ok(catalogUseCase.addReview(id, user.getUserId(), rating, comment));
    }

    @GetMapping("/catalog/{id}/reviews")
    public ResponseEntity<List<CatalogReview>> getReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogUseCase.getReviews(id, page, size));
    }

    // Admin endpoints
    @PostMapping("/catalog/{id}/approve")
    @RequiresPermission(Permission.ADMIN_FULL)
    public ResponseEntity<CatalogEntry> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogUseCase.approve(id));
    }

    @PostMapping("/catalog/{id}/reject")
    @RequiresPermission(Permission.ADMIN_FULL)
    public ResponseEntity<CatalogEntry> reject(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(catalogUseCase.reject(id, body.get("reason")));
    }
}
