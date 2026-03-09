package com.ia.aggregator.infrastructure.ai.auth;

import java.net.http.HttpRequest;

/**
 * Bearer token authentication strategy.
 * Adds "Authorization: Bearer {token}" header.
 * Used by: OpenAI, Anthropic, Gemini, most providers.
 */
public class BearerTokenAuth implements AuthStrategy {

    private final String token;

    public BearerTokenAuth(String token) {
        this.token = token;
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
