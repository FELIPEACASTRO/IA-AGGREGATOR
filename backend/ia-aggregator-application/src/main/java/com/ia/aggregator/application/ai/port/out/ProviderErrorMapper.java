package com.ia.aggregator.application.ai.port.out;

import com.ia.aggregator.common.exception.TechnicalException;

/**
 * SPI that converts a raw provider HTTP error response into a typed
 * {@link AiProviderException} with a classified {@code failureCategory}.
 *
 * <p>Failure categories (from prompt specification):
 * <ul>
 *   <li>{@code auth} — 401/403 authentication or authorization failure</li>
 *   <li>{@code validation} — 400 bad request / invalid parameters</li>
 *   <li>{@code rate_limit} — 429 too many requests</li>
 *   <li>{@code upstream_4xx} — other 4xx from the provider</li>
 *   <li>{@code upstream_5xx} — 5xx server errors from the provider</li>
 *   <li>{@code timeout} — connect or read timeout</li>
 *   <li>{@code parsing} — response parsing / deserialization failure</li>
 *   <li>{@code compliance_block} — threat intel request blocked by compliance gate</li>
 * </ul>
 *
 * <p>Big O: O(1) per error classification.
 */
public interface ProviderErrorMapper {

    /**
     * Maps a raw provider error to a classified {@link AiProviderException}.
     *
     * @param httpStatus   HTTP status code from the provider (or 0 for timeouts)
     * @param responseBody raw response body string (may be empty for network errors)
     * @param providerName provider that produced the error (for logging context)
     * @return classified exception, never null
     */
    TechnicalException toTechnicalException(int httpStatus, String responseBody, String providerName);
}
