package com.ia.aggregator.application.ai.port.in;

import com.ia.aggregator.application.ai.dto.ThreatIntelRequest;
import com.ia.aggregator.application.ai.dto.ThreatIntelResult;

/**
 * Input port for threat intelligence search.
 * Searches dark web and threat intelligence sources.
 * Subject to compliance gate verification.
 */
public interface ThreatIntelUseCase {
    ThreatIntelResult execute(ThreatIntelRequest request);
}
