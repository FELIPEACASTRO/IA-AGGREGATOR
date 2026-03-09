package com.ia.aggregator.application.ai.port.out.capability;

import com.ia.aggregator.application.ai.dto.ThreatIntelRequest;
import com.ia.aggregator.application.ai.dto.ThreatIntelResult;

/**
 * Capability interface for dark web and threat intelligence search.
 *
 * <p><strong>Compliance requirement:</strong> All providers implementing this interface
 * MUST be gated behind the {@code security.compliance.dark-web-enabled} feature flag.
 * Access to threat intelligence data requires explicit organizational authorization.
 */
public interface ThreatIntelCapable {

    /**
     * Searches threat intelligence sources for the given query.
     *
     * @param request the threat intel request with query, date range, and source filters
     * @return the result with threat intelligence hits and metadata
     */
    ThreatIntelResult searchThreatIntel(ThreatIntelRequest request);
}
