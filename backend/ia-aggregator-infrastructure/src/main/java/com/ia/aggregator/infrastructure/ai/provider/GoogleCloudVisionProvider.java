package com.ia.aggregator.infrastructure.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.aggregator.application.ai.dto.*;
import com.ia.aggregator.application.ai.port.out.capability.OcrCapable;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.common.exception.TechnicalException;
import com.ia.aggregator.domain.ai.Capability;
import com.ia.aggregator.infrastructure.ai.auth.OAuth2ClientCredentialsAuth;
import com.ia.aggregator.infrastructure.ai.provider.base.AbstractGoogleCloudProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Google Cloud Vision provider — OCR / text extraction from images.
 *
 * <p>API: {@code https://vision.googleapis.com/v1/images:annotate}
 * <p>Supports: OCR
 * <p>Auth: OAuth2 Client Credentials
 *
 * <p>Request: {@code {"requests": [{"image": {"content": "<base64>"}, "features": [{"type": "TEXT_DETECTION"}]}]}}
 * <p>Response: {@code {"responses": [{"fullTextAnnotation": {"text": "..."}}]}}
 *
 * <p>Big O: O(1) per request — synchronous.
 */
@Component
@ConditionalOnProperty(name = "app.ai.providers.google-cloud.client-id")
public class GoogleCloudVisionProvider extends AbstractGoogleCloudProvider
        implements OcrCapable {

    private static final String PROVIDER_NAME = "google-cloud-vision";
    private static final Set<Capability> SUPPORTED_CAPABILITIES =
            EnumSet.of(Capability.OCR);
    private static final String VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate";

    public GoogleCloudVisionProvider(
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
                        "https://www.googleapis.com/auth/cloud-vision"),
                projectId, timeoutMs, retryAttempts, retryBackoffMs,
                List.of("TEXT_DETECTION", "DOCUMENT_TEXT_DETECTION"));
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public Set<Capability> capabilities() {
        return SUPPORTED_CAPABILITIES;
    }

    @Override
    public OcrResult ocr(OcrRequest request) {
        String featureType = request.model() != null ? request.model() : "TEXT_DETECTION";

        Map<String, Object> imageSource = new LinkedHashMap<>();
        if (request.imageData() != null) {
            imageSource.put("content", Base64.getEncoder().encodeToString(request.imageData()));
        } else if (request.imageUrl() != null) {
            imageSource.put("source", Map.of("imageUri", request.imageUrl()));
        } else {
            throw new TechnicalException(ErrorCode.AI_016, "Either imageData or imageUrl must be provided");
        }

        Map<String, Object> annotateRequest = Map.of(
                "image", imageSource,
                "features", List.of(Map.of("type", featureType)),
                "imageContext", request.language() != null
                        ? Map.of("languageHints", List.of(request.language()))
                        : Map.of()
        );

        Map<String, Object> body = Map.of("requests", List.of(annotateRequest));

        JsonNode response = executeWithRetry(() -> sendJsonPost(VISION_API_URL, body));

        JsonNode firstResponse = response.path("responses").path(0);
        if (firstResponse.has("error")) {
            String errorMsg = firstResponse.path("error").path("message").asText("Unknown Vision error");
            throw new TechnicalException(ErrorCode.AI_016, PROVIDER_NAME + " error: " + errorMsg);
        }

        String fullText = firstResponse.path("fullTextAnnotation").path("text").asText("");

        List<OcrResult.TextBlock> blocks = new ArrayList<>();
        JsonNode annotations = firstResponse.path("textAnnotations");
        if (annotations.isArray()) {
            for (int i = 1; i < annotations.size(); i++) {
                JsonNode ann = annotations.get(i);
                String blockText = ann.path("description").asText("");
                Double confidence = ann.has("confidence") ? ann.path("confidence").asDouble() : null;
                blocks.add(new OcrResult.TextBlock(blockText, null, confidence));
            }
        }

        Double overallConfidence = null;
        JsonNode pages = firstResponse.path("fullTextAnnotation").path("pages");
        if (pages.isArray() && pages.size() > 0) {
            double conf = pages.get(0).path("confidence").asDouble(0);
            if (conf > 0) overallConfidence = conf;
        }

        return new OcrResult(fullText, featureType, PROVIDER_NAME, blocks, overallConfidence);
    }
}
