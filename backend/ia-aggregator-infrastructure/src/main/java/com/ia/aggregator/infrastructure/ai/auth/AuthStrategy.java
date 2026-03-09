package com.ia.aggregator.infrastructure.ai.auth;

import java.net.http.HttpRequest;

/**
 * Strategy interface for provider authentication.
 *
 * <p>Design Pattern: Strategy — each implementation provides a different
 * authentication mechanism (Bearer, API-Key header, Query param, AWS SigV4, OAuth2).
 *
 * <p>SOLID: Open/Closed — new auth strategies can be added without modifying
 * existing providers.
 */
public interface AuthStrategy {

    /**
     * Applies authentication to an HTTP request builder.
     *
     * @param builder the HttpRequest.Builder to add auth headers/params to
     * @return the builder with authentication applied
     */
    HttpRequest.Builder apply(HttpRequest.Builder builder);
}
