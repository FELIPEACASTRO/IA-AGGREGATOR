package com.ia.aggregator.domain.ai;

/**
 * Implementation and operational status of a provider integration.
 *
 * <p>Providers must be classified honestly using this enum.
 * Rule: never declare IMPLEMENTED without real build and test evidence.
 * Big O: O(1) for all enum operations.
 */
public enum ProviderStatus {

    /**
     * Provider is fully implemented, compiled, and has passing WireMock tests.
     * May have external blockers (credentials, plans) that prevent live integration.
     */
    IMPLEMENTED,

    /**
     * Provider is implemented but with known limitations:
     * - Capability mapping is approximate (e.g., Google Cloud NLP as WEB_SEARCH)
     * - Requires paid subscription or enterprise access
     * - Partial feature coverage (e.g., only some models supported)
     */
    IMPLEMENTED_WITH_RESTRICTIONS,

    /**
     * Provider code exists but cannot be verified online due to:
     * - No public API access or enterprise-only plans
     * - Missing documentation
     * - API endpoint not publicly accessible
     * Must include exact blocker cause in documentation.
     */
    BLOCKED,

    /**
     * Intentionally excluded from integration scope.
     * Example: Midjourney — no official public API available.
     */
    OUT_OF_SCOPE
}
