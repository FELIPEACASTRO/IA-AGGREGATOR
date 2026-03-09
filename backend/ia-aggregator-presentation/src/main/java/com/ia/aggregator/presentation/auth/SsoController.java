package com.ia.aggregator.presentation.auth;

import com.ia.aggregator.application.auth.port.in.SsoUseCase;
import com.ia.aggregator.domain.auth.vo.AuthProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SSO/OAuth endpoints for Google and GitHub login.
 */
@RestController
@RequestMapping("/api/v1/auth/sso")
public class SsoController {

    private final SsoUseCase ssoUseCase;

    public SsoController(SsoUseCase ssoUseCase) {
        this.ssoUseCase = ssoUseCase;
    }

    @GetMapping("/{provider}/authorize")
    public ResponseEntity<Map<String, String>> authorize(
            @PathVariable String provider,
            @RequestParam String redirectUri) {
        AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());
        String url = ssoUseCase.getAuthorizationUrl(authProvider, redirectUri);
        return ResponseEntity.ok(Map.of("authorizationUrl", url));
    }

    @PostMapping("/{provider}/callback")
    public ResponseEntity<SsoUseCase.SsoResult> callback(
            @PathVariable String provider,
            @RequestBody Map<String, String> body) {
        AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());
        String code = body.get("code");
        String redirectUri = body.get("redirectUri");

        SsoUseCase.SsoResult result = ssoUseCase.exchangeCode(authProvider, code, redirectUri);
        return ResponseEntity.ok(result);
    }
}
