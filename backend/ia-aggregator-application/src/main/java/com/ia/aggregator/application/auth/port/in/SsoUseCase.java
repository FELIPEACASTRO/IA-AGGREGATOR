package com.ia.aggregator.application.auth.port.in;

import com.ia.aggregator.domain.auth.vo.AuthProvider;

/**
 * Use case for SSO/OAuth authentication flows.
 */
public interface SsoUseCase {

    /**
     * Get the authorization URL for the given provider.
     *
     * @param provider the OAuth provider (GOOGLE, GITHUB, etc.)
     * @param redirectUri where to redirect after auth
     * @return the authorization URL to redirect the user to
     */
    String getAuthorizationUrl(AuthProvider provider, String redirectUri);

    /**
     * Exchange an authorization code for tokens.
     *
     * @param provider the OAuth provider
     * @param code     the authorization code from the callback
     * @param redirectUri the redirect URI used in the auth request
     * @return the authentication result with tokens
     */
    SsoResult exchangeCode(AuthProvider provider, String code, String redirectUri);

    record SsoResult(String accessToken, String refreshToken, long expiresIn,
                     boolean newUser, String email, String fullName) {}
}
