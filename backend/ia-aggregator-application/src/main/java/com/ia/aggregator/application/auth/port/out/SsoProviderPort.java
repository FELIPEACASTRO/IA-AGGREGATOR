package com.ia.aggregator.application.auth.port.out;

import com.ia.aggregator.domain.auth.vo.AuthProvider;

/**
 * Port for SSO/OAuth provider interactions.
 */
public interface SsoProviderPort {

    /**
     * Build the authorization URL for the provider.
     */
    String getAuthorizationUrl(AuthProvider provider, String redirectUri);

    /**
     * Exchange authorization code for user info.
     */
    OAuthUserInfo exchangeCodeForUser(AuthProvider provider, String code, String redirectUri);

    /**
     * User information retrieved from the OAuth provider.
     */
    record OAuthUserInfo(
            String email,
            String name,
            String providerId,
            String avatarUrl
    ) {}
}
