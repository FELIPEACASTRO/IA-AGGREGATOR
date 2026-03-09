package com.ia.aggregator.application.billing.port.out;

import java.util.Map;

/**
 * Port for payment gateway operations (Stripe).
 */
public interface PaymentGatewayPort {

    String createCustomer(String email, String name, Map<String, String> metadata);

    String createSubscription(String customerId, String priceId);

    void cancelSubscription(String subscriptionId);

    String changeSubscription(String subscriptionId, String newPriceId);

    boolean verifyWebhookSignature(String payload, String signature);

    /**
     * Report usage to the payment gateway for metered billing.
     */
    void reportUsage(String subscriptionItemId, long quantity, long timestamp);
}
