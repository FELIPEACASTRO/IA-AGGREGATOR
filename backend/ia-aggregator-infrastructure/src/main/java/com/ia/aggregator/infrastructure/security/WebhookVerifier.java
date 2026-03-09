package com.ia.aggregator.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Webhook signature verifier supporting GitHub, Slack, and Linear HMAC schemes.
 *
 * <p>Uses constant-time comparison to prevent timing attacks.
 */
@Component
public class WebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookVerifier.class);

    /**
     * Verify GitHub webhook signature (X-Hub-Signature-256).
     *
     * @param payload the raw request body
     * @param signature the X-Hub-Signature-256 header value (sha256=...)
     * @param secret the webhook secret
     * @return true if signature is valid
     */
    public boolean verifyGitHub(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }
        if (!signature.startsWith("sha256=")) {
            return false;
        }

        String expected = "sha256=" + hmacSha256(payload, secret);
        return constantTimeEquals(expected, signature);
    }

    /**
     * Verify Slack webhook signature (X-Slack-Signature).
     *
     * @param payload the raw request body
     * @param timestamp the X-Slack-Request-Timestamp header
     * @param signature the X-Slack-Signature header (v0=...)
     * @param signingSecret the Slack signing secret
     * @return true if signature is valid
     */
    public boolean verifySlack(String payload, String timestamp, String signature, String signingSecret) {
        if (payload == null || timestamp == null || signature == null || signingSecret == null) {
            return false;
        }

        // Check timestamp is within 5 minutes to prevent replay attacks
        long ts = Long.parseLong(timestamp);
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - ts) > 300) {
            log.warn("Slack webhook timestamp too old: {} vs {}", ts, now);
            return false;
        }

        String baseString = "v0:" + timestamp + ":" + payload;
        String expected = "v0=" + hmacSha256(baseString, signingSecret);
        return constantTimeEquals(expected, signature);
    }

    /**
     * Verify Linear webhook signature (Linear-Signature).
     *
     * @param payload the raw request body
     * @param signature the Linear-Signature header
     * @param secret the webhook secret
     * @return true if signature is valid
     */
    public boolean verifyLinear(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }

        String expected = hmacSha256(payload, secret);
        return constantTimeEquals(expected, signature);
    }

    /**
     * Generic HMAC-SHA256 verification.
     */
    public boolean verifyHmacSha256(String payload, String expectedSignature, String secret) {
        if (payload == null || expectedSignature == null || secret == null) {
            return false;
        }
        String computed = hmacSha256(payload, secret);
        return constantTimeEquals(computed, expectedSignature);
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
