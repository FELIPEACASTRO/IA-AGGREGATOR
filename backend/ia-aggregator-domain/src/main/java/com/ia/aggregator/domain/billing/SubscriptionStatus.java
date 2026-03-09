package com.ia.aggregator.domain.billing;

/**
 * Subscription lifecycle states.
 */
public enum SubscriptionStatus {
    ACTIVE,
    TRIALING,
    PAST_DUE,
    CANCELED,
    PAUSED,
    EXPIRED
}
