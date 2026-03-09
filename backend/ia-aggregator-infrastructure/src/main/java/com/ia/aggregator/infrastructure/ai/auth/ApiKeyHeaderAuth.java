package com.ia.aggregator.infrastructure.ai.auth;

import java.net.http.HttpRequest;

/**
 * API key header authentication strategy.
 * Adds a custom header with the API key (e.g., "x-api-key", "api-key").
 * Used by: Anthropic (x-api-key), AI21, some cloud providers.
 */
public class ApiKeyHeaderAuth implements AuthStrategy {

    private final String headerName;
    private final String apiKey;

    public ApiKeyHeaderAuth(String headerName, String apiKey) {
        this.headerName = headerName;
        this.apiKey = apiKey;
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        return builder.header(headerName, apiKey);
    }
}
