package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.OAuth2ClientCredentialsAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractGoogleCloudProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Google Cloud Translation provider — text translation via Cloud Translation API v3.
 *
 * <p>API: {@code https://translation.googleapis.com/v3/projects/{project}/locations/global:translateText}
 * <p>Supports: CHAT (text-in → translated-text-out)
 * <p>Auth: OAuth2 Client Credentials
 *
 * <p>Mapping to CHAT capability: the prompt is the text to translate,
 * the model specifies the target language code (e.g., "pt", "es", "fr").
 *
 * <p>Big O: O(1) per request — synchronous.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.google-cloud.client-id")
public class GoogleCloudTranslationProvider extends AbstractGoogleCloudProvider {

    private static final String PROVIDER_NAME = "google-cloud-translation";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.CHAT);
    private static final String TRANSLATION_API_BASE = "https://translation.googleapis.com/v3";

    public GoogleCloudTranslationProvider(
            ObjectMapper objectMapper,
            @Value("${app.ai.providers.google-cloud.token-endpoint:https://oauth2.googleapis.com/token}") String tokenEndpoint,
            @Value("${app.ai.providers.google-cloud.client-id:}") String clientId,
            @Value("${app.ai.providers.google-cloud.client-secret:}") String clientSecret,
            @Value("${app.ai.providers.google-cloud.project-id:}") String projectId,
            @Value("${app.ai.providers.google-cloud.timeout-ms:30000}") long timeoutMs,
            @Value("${app.ai.providers.google-cloud.retry-attempts:2}") int retryAttempts,
            @Value("${app.ai.providers.google-cloud.retry-backoff-ms:1000}") long retryBackoffMs
    ) {
        super(objectMapper,
                new OAuth2ClientCredentialsAuth(tokenEndpoint, clientId, clientSecret,
                        "https://www.googleapis.com/auth/cloud-translation"),
                projectId, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("pt", "es", "fr", "de", "ja", "zh", "ko", "ar", "hi", "ru"));
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<Capability> capabilities() {
        return SUPPORTED_CAPABILITIES;
    }

    /**
     * Translates text using Google Cloud Translation.
     *
     * @param prompt the text to translate
     * @param model the target language code (e.g., "pt", "es", "fr")
     * @return the translated text
     */
    @Override
    public String generate(String prompt, String model) {
        String targetLang = model != null ? model : "en";

        String endpoint = TRANSLATION_API_BASE + "/projects/" + projectId
                + "/locations/global:translateText";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(prompt));
        body.put("targetLanguageCode", targetLang);
        body.put("mimeType", "text/plain");

        JsonNode response = executeWithRetry(() -> sendJsonPost(endpoint, body));

        JsonNode translations = response.path("translations");
        if (translations.isArray() && translations.size() > 0) {
            return translations.get(0).path("translatedText").asText("");
        }

        return "";
    }
}
