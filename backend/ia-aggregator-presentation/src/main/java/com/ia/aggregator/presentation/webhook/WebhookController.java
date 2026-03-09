package com.ia.aggregator.presentation.webhook;

import com.ia.aggregator.infrastructure.security.WebhookVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook ingestion endpoints for GitHub, Slack, Linear, and Stripe.
 *
 * <p>All payloads are verified via HMAC before processing.
 * Endpoints are excluded from JWT auth (verified by signature instead).
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookVerifier verifier;
    private final String githubSecret;
    private final String slackSigningSecret;
    private final String linearSecret;

    public WebhookController(
            WebhookVerifier verifier,
            @Value("${app.security.webhooks.github-secret:}") String githubSecret,
            @Value("${app.security.webhooks.slack-signing-secret:}") String slackSigningSecret,
            @Value("${app.security.webhooks.linear-webhook-secret:}") String linearSecret) {
        this.verifier = verifier;
        this.githubSecret = githubSecret;
        this.slackSigningSecret = slackSigningSecret;
        this.linearSecret = linearSecret;
    }

    @PostMapping("/github")
    public ResponseEntity<Map<String, Object>> handleGitHub(
            @RequestBody String payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event) {

        if (githubSecret.isBlank() || !verifier.verifyGitHub(payload, signature, githubSecret)) {
            log.warn("Invalid GitHub webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        log.info("GitHub webhook received: event={}", event);
        // Dispatch to domain event handlers
        return ResponseEntity.ok(Map.of("status", "accepted", "event", String.valueOf(event)));
    }

    @PostMapping("/slack")
    public ResponseEntity<Map<String, Object>> handleSlack(
            @RequestBody String payload,
            @RequestHeader(value = "X-Slack-Signature", required = false) String signature,
            @RequestHeader(value = "X-Slack-Request-Timestamp", required = false) String timestamp) {

        if (slackSigningSecret.isBlank()
                || !verifier.verifySlack(payload, timestamp, signature, slackSigningSecret)) {
            log.warn("Invalid Slack webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        log.info("Slack webhook received");
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }

    @PostMapping("/linear")
    public ResponseEntity<Map<String, Object>> handleLinear(
            @RequestBody String payload,
            @RequestHeader(value = "Linear-Signature", required = false) String signature) {

        if (linearSecret.isBlank() || !verifier.verifyLinear(payload, signature, linearSecret)) {
            log.warn("Invalid Linear webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid signature"));
        }

        log.info("Linear webhook received");
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}
