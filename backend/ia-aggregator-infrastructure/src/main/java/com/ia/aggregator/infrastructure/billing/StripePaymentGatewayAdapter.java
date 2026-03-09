package com.ia.aggregator.infrastructure.billing;

import com.ia.aggregator.application.billing.port.out.PaymentGatewayPort;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Stripe payment gateway adapter.
 */
@Component
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGatewayAdapter.class);
    private static final String STRIPE_API = "https://api.stripe.com/v1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.billing.stripe.secret-key:}")
    private String stripeSecretKey;
    @Value("${app.security.webhooks.stripe-webhook-secret:}")
    private String webhookSecret;

    public StripePaymentGatewayAdapter(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    @Override
    public String createCustomer(String email, String name, Map<String, String> metadata) {
        String body = "email=" + encode(email) + "&name=" + encode(name);
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            body += "&metadata[" + encode(entry.getKey()) + "]=" + encode(entry.getValue());
        }
        JsonNode response = stripePost("/customers", body);
        return response.get("id").asText();
    }

    @Override
    public String createSubscription(String customerId, String priceId) {
        String body = "customer=" + encode(customerId) + "&items[0][price]=" + encode(priceId);
        JsonNode response = stripePost("/subscriptions", body);
        return response.get("id").asText();
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        stripeDelete("/subscriptions/" + subscriptionId);
    }

    @Override
    public String changeSubscription(String subscriptionId, String newPriceId) {
        // Get current subscription to find the item ID
        JsonNode sub = stripeGet("/subscriptions/" + subscriptionId);
        String itemId = sub.get("items").get("data").get(0).get("id").asText();

        String body = "items[0][id]=" + encode(itemId) + "&items[0][price]=" + encode(newPriceId);
        JsonNode response = stripePost("/subscriptions/" + subscriptionId, body);
        return response.get("id").asText();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) return false;
        try {
            // Stripe uses t=timestamp,v1=signature format
            String[] parts = signature.split(",");
            String timestamp = null;
            String sig = null;
            for (String part : parts) {
                if (part.startsWith("t=")) timestamp = part.substring(2);
                if (part.startsWith("v1=")) sig = part.substring(3);
            }
            if (timestamp == null || sig == null) return false;

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(expected.getBytes(), sig.getBytes());
        } catch (Exception e) {
            log.warn("Stripe webhook verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void reportUsage(String subscriptionItemId, long quantity, long timestamp) {
        String body = "quantity=" + quantity + "&timestamp=" + timestamp;
        stripePost("/subscription_items/" + subscriptionItemId + "/usage_records", body);
    }

    private JsonNode stripePost(String path, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STRIPE_API + path))
                    .header("Authorization", "Bearer " + stripeSecretKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new TechnicalException(ErrorCode.BILL_004, "Stripe API error: " + e.getMessage(), e);
        }
    }

    private JsonNode stripeGet(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STRIPE_API + path))
                    .header("Authorization", "Bearer " + stripeSecretKey)
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new TechnicalException(ErrorCode.BILL_004, "Stripe API error: " + e.getMessage(), e);
        }
    }

    private void stripeDelete(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STRIPE_API + path))
                    .header("Authorization", "Bearer " + stripeSecretKey)
                    .DELETE().build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new TechnicalException(ErrorCode.BILL_004, "Stripe API error: " + e.getMessage(), e);
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
