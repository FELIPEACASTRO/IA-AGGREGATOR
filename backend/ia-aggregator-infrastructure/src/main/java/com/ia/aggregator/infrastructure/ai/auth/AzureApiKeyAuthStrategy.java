package com.ia.aggregator.infrastructure.ai.auth;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * Azure OpenAI authentication strategy.
 *
 * <p>Azure OpenAI uses a distinct authentication pattern from public OpenAI:
 * <ul>
 *   <li>Header: {@code api-key: {key}} (not {@code Authorization: Bearer})</li>
 *   <li>Query param: {@code api-version={version}} on every request</li>
 * </ul>
 *
 * <p>This strategy handles the {@code api-key} header. The {@code api-version} query param
 * is typically appended to the endpoint URL by {@code AzureOpenAiProvider} directly,
 * or via a {@link CompositeAuthStrategy} combining this with a {@link QueryParamAuth}.
 *
 * <p>IMPORTANT: This is NOT the same as {@link BearerTokenAuth} — Azure rejects
 * {@code Authorization: Bearer} headers in favor of its own {@code api-key} header.
 *
 * <p>Big O: O(1) per request.
 */
public class AzureApiKeyAuthStrategy implements AuthStrategy {

    /** Azure OpenAI uses "api-key" as the header name (distinct from Bearer auth). */
    private static final String AZURE_API_KEY_HEADER = "api-key";

    private final String apiKey;

    /**
     * @param apiKey the Azure OpenAI resource API key (from Azure Portal → Keys and Endpoint)
     */
    public AzureApiKeyAuthStrategy(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public HttpRequest.Builder apply(HttpRequest.Builder builder) {
        return builder.header(AZURE_API_KEY_HEADER, apiKey);
    }

    /**
     * Appends the required {@code api-version} query parameter to a URI.
     *
     * <p>Azure requires this on every request:
     * {@code https://{resource}.openai.azure.com/openai/deployments/{deployment}/chat/completions?api-version=2024-02-01}
     *
     * @param baseUri    the URI to append to
     * @param apiVersion the Azure API version string (e.g., {@code "2024-02-01"})
     * @return URI with api-version appended
     */
    public static URI appendApiVersion(URI baseUri, String apiVersion) {
        String separator = baseUri.getQuery() == null ? "?" : "&";
        return URI.create(baseUri + separator + "api-version=" + apiVersion);
    }
}
