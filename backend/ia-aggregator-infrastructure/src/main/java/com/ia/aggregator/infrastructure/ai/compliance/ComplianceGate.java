package com.ia.aggregator.infrastructure.ai.compliance;

import com.ia.aggregator.application.ai.port.out.ComplianceCheckPort;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Compliance gate for sensitive provider operations (e.g., threat intelligence).
 *
 * <p>Implements {@link ComplianceCheckPort} to satisfy the application layer's
 * hexagonal architecture port contract.
 *
 * <p>Enforces feature flags and compliance requirements before allowing
 * access to dark web / threat intel providers.
 *
 * <p>Design: Gate pattern — all threat intel requests pass through this gate
 * before reaching the actual provider.
 */
@Component
public class ComplianceGate implements ComplianceCheckPort {

    private final boolean darkWebEnabled;
    private final boolean auditLogEnabled;

    public ComplianceGate(
            @Value("${app.ai.compliance.dark-web-enabled:false}") boolean darkWebEnabled,
            @Value("${app.ai.compliance.threat-intel-audit-log:true}") boolean auditLogEnabled
    ) {
        this.darkWebEnabled = darkWebEnabled;
        this.auditLogEnabled = auditLogEnabled;
    }

    /**
     * Checks if dark web / threat intelligence access is enabled.
     *
     * @throws TechnicalException with AI_020 if disabled
     */
    public void requireDarkWebAccess() {
        if (!darkWebEnabled) {
            throw new TechnicalException(ErrorCode.AI_020,
                    "Threat intelligence search is disabled. "
                    + "Set 'app.ai.compliance.dark-web-enabled=true' to enable.");
        }
    }

    /**
     * Returns whether threat intel audit logging is enabled.
     */
    public boolean isAuditLogEnabled() {
        return auditLogEnabled;
    }

    /**
     * Returns whether dark web access is enabled.
     */
    public boolean isDarkWebEnabled() {
        return darkWebEnabled;
    }
}
