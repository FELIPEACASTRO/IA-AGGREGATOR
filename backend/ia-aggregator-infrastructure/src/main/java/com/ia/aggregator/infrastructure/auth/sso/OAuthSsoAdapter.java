package com.ia.aggregator.infrastructure.auth.sso;

import com.ia.aggregator.application.auth.port.out.SsoProviderPort;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.domain.auth.vo.AuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OAuth2 SSO adapter supporting Google and GitHub.
 */
@Component
public class OAuthSsoAdapter implements SsoProviderPort {

    private static final Logger log = LoggerFactory.getLogger(OAuthSsoAdapter.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.sso.google.client-id:}")
    private String googleClientId;
    @Value("${app.sso.google.client-secret:}")
    private String googleClientSecret;
    @Value("${app.sso.github.client-id:}")
    private String githubClientId;
    @Value("${app.sso.github.client-secret:}")
    private String githubClientSecret;

    public OAuthSsoAdapter(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getAuthorizationUrl(AuthProvider provider, String redirectUri) {
        return switch (provider) {
            case GOOGLE -> "https://accounts.google.com/o/oauth2/v2/auth?"
                    + "client_id=" + encode(googleClientId)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&response_type=code"
                    + "&scope=" + encode("openid email profile")
                    + "&access_type=offline";

            case GITHUB -> "https://github.com/login/oauth/authorize?"
                    + "client_id=" + encode(githubClientId)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&scope=" + encode("user:email read:user");

            default -> throw new TechnicalException(ErrorCode.AUTH_007,
                    "SSO not supported for provider: " + provider);
        };
    }

    @Override
    public OAuthUserInfo exchangeCodeForUser(AuthProvider provider, String code, String redirectUri) {
        return switch (provider) {
            case GOOGLE -> exchangeGoogle(code, redirectUri);
            case GITHUB -> exchangeGithub(code, redirectUri);
            default -> throw new TechnicalException(ErrorCode.AUTH_007,
                    "SSO exchange not supported for: " + provider);
        };
    }

    private OAuthUserInfo exchangeGoogle(String code, String redirectUri) {
        try {
            // Exchange code for token
            String tokenBody = "code=" + encode(code)
                    + "&client_id=" + encode(googleClientId)
                    + "&client_secret=" + encode(googleClientSecret)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&grant_type=authorization_code";

            HttpRequest tokenReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                    .build();

            HttpResponse<String> tokenResp = httpClient.send(tokenReq, HttpResponse.BodyHandlers.ofString());
            JsonNode tokenJson = objectMapper.readTree(tokenResp.body());
            String accessToken = tokenJson.get("access_token").asText();

            // Get user info
            HttpRequest userReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().build();

            HttpResponse<String> userResp = httpClient.send(userReq, HttpResponse.BodyHandlers.ofString());
            JsonNode userJson = objectMapper.readTree(userResp.body());

            return new OAuthUserInfo(
                    userJson.path("email").asText(),
                    userJson.path("name").asText(),
                    userJson.path("id").asText(),
                    userJson.path("picture").asText(null)
            );
        } catch (Exception e) {
            log.error("Google SSO exchange failed: {}", e.getMessage());
            throw new TechnicalException(ErrorCode.AUTH_007, "Google SSO failed: " + e.getMessage(), e);
        }
    }

    private OAuthUserInfo exchangeGithub(String code, String redirectUri) {
        try {
            // Exchange code for token
            String tokenBody = objectMapper.writeValueAsString(java.util.Map.of(
                    "client_id", githubClientId,
                    "client_secret", githubClientSecret,
                    "code", code,
                    "redirect_uri", redirectUri
            ));

            HttpRequest tokenReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://github.com/login/oauth/access_token"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                    .build();

            HttpResponse<String> tokenResp = httpClient.send(tokenReq, HttpResponse.BodyHandlers.ofString());
            JsonNode tokenJson = objectMapper.readTree(tokenResp.body());
            String accessToken = tokenJson.get("access_token").asText();

            // Get user info
            HttpRequest userReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/user"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET().build();

            HttpResponse<String> userResp = httpClient.send(userReq, HttpResponse.BodyHandlers.ofString());
            JsonNode userJson = objectMapper.readTree(userResp.body());

            // Get primary email
            HttpRequest emailReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/user/emails"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET().build();

            HttpResponse<String> emailResp = httpClient.send(emailReq, HttpResponse.BodyHandlers.ofString());
            JsonNode emails = objectMapper.readTree(emailResp.body());
            String email = null;
            for (JsonNode e : emails) {
                if (e.path("primary").asBoolean()) {
                    email = e.path("email").asText();
                    break;
                }
            }

            return new OAuthUserInfo(
                    email != null ? email : userJson.path("email").asText(),
                    userJson.path("name").asText(userJson.path("login").asText()),
                    String.valueOf(userJson.path("id").asLong()),
                    userJson.path("avatar_url").asText(null)
            );
        } catch (Exception e) {
            log.error("GitHub SSO exchange failed: {}", e.getMessage());
            throw new TechnicalException(ErrorCode.AUTH_007, "GitHub SSO failed: " + e.getMessage(), e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
