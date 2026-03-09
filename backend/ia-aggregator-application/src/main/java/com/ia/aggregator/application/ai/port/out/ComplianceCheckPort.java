package com.ia.aggregator.application.ai.port.out;

/**
 * Port for compliance verification before sensitive operations.
 *
 * <p>Allows the application layer to enforce compliance requirements
 * (e.g., dark web access, threat intelligence) without depending on
 * infrastructure-level feature flags.
 *
 * <p>Design: Port (Hexagonal Architecture) — the infrastructure's
 * ComplianceGate implements this port.
 */
public interface ComplianceCheckPort {

    /**
     * Validates that dark web / threat intelligence access is enabled.
     *
     * @throws com.ia.aggregator.common.exception.TechnicalException if access is disabled
     */
    void requireDarkWebAccess();

    /**
     * Returns whether threat intel audit logging is enabled.
     */
    boolean isAuditLogEnabled();
}
