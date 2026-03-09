package com.ia.aggregator.infrastructure.ai.auth;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * Query parameter authentication strategy.
 * Appends the API key as a URL query parameter (e.g., "?key=xxx").
 * Used by: Some legacy providers, Google API services.
 */
public class QueryParamAuth implements AuthStrategy {

    private final String paramName;
    private final String apiKey;

    public QueryParamAuth(String paramName, String apiKey) {
        this.paramName = paramName;
        this.apiKey = apiKey;
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        // Note: This strategy requires the URI to be modified before building.
        // The provider should call applyToUri() to get the modified URI.
        return builder;
    }

    /**
     * Appends the API key as a query parameter to the given URI.
     *
     * @param uri the original URI
     * @return the URI with the auth query parameter appended
     */
    public URI applyToUri(URI uri) {
        String query = uri.getQuery();
        String separator = (query == null || query.isEmpty()) ? "?" : "&";
        return URI.create(uri.toString() + separator + paramName + "=" + apiKey);
    }
}
