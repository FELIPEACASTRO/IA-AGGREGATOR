package com.ia.aggregator.domain.ai;

/**
 * Enumerates authentication strategies used by AI providers.
 *
 * <p>Maps 1:1 to concrete {@code AuthStrategy} implementations in the infrastructure layer.
 * Big O: O(1) for all enum operations.
 */
public enum AuthType {

    /** {@code Authorization: Bearer {token}} header — most common pattern */
    BEARER_TOKEN,

    /** Custom header name with API key (e.g., {@code x-api-key}, {@code xi-api-key}) */
    API_KEY_HEADER,

    /** API key passed as a URL query parameter (e.g., Gemini {@code ?key={key}}) */
    QUERY_PARAM,

    /** AWS Signature Version 4 — used exclusively by AWS Bedrock */
    AWS_SIGV4,

    /** OAuth 2.0 client credentials flow — used by Google Cloud and Azure Entra */
    OAUTH2_CLIENT_CREDENTIALS,

    /** Composite: applies multiple auth strategies in sequence (e.g., Azure api-key + api-version) */
    COMPOSITE
}
