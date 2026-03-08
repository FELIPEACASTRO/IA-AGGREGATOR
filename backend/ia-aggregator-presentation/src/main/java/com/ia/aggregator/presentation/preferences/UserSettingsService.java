package com.ia.aggregator.presentation.preferences;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ia.aggregator.common.exception.BusinessException;
import com.ia.aggregator.common.exception.ErrorCode;
import com.ia.aggregator.infrastructure.auth.persistence.entity.NotificationPreferenceJpaEntity;
import com.ia.aggregator.infrastructure.auth.persistence.entity.UserJpaEntity;
import com.ia.aggregator.infrastructure.auth.persistence.repository.NotificationPreferenceJpaRepository;
import com.ia.aggregator.infrastructure.auth.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserSettingsService {

    private final UserJpaRepository userJpaRepository;
    private final NotificationPreferenceJpaRepository notificationPreferenceJpaRepository;
    private final ObjectMapper objectMapper;

    public UserSettingsService(UserJpaRepository userJpaRepository,
                               NotificationPreferenceJpaRepository notificationPreferenceJpaRepository,
                               ObjectMapper objectMapper) {
        this.userJpaRepository = userJpaRepository;
        this.notificationPreferenceJpaRepository = notificationPreferenceJpaRepository;
        this.objectMapper = objectMapper;
    }

    public UserPreferencesResponse getPreferences(UUID userId) {
        UserJpaEntity user = getUser(userId);
        ObjectNode metadata = parseMetadata(user.getMetadata());

        return new UserPreferencesResponse(
                user.getFullName(),
                user.getLocale(),
                metadata.path("theme").asText("dark"),
                metadata.path("chatFontMode").asText("default")
        );
    }

    public UserPreferencesResponse updatePreferences(UUID userId, UserPreferencesRequest request) {
        UserJpaEntity user = getUser(userId);
        user.setFullName(request.fullName().trim());
        user.setLocale(request.locale().trim());

        ObjectNode metadata = parseMetadata(user.getMetadata());
        metadata.put("theme", request.theme());
        metadata.put("chatFontMode", request.chatFontMode());
        user.setMetadata(writeMetadata(metadata));

        userJpaRepository.save(user);
        return getPreferences(userId);
    }

    public NotificationPreferencesResponse getNotificationPreferences(UUID userId) {
        NotificationPreferenceJpaEntity entity = getOrCreateNotificationPreferences(userId);
        return toNotificationResponse(entity);
    }

    public NotificationPreferencesResponse updateNotificationPreferences(UUID userId, NotificationPreferencesRequest request) {
        NotificationPreferenceJpaEntity entity = getOrCreateNotificationPreferences(userId);
        entity.setEmailEnabled(request.emailEnabled());
        entity.setPushEnabled(request.pushEnabled());
        entity.setBillingAlerts(request.billingAlerts());
        entity.setUsageAlerts(request.usageAlerts());
        entity.setSecurityAlerts(request.securityAlerts());
        entity.setProductUpdates(request.productUpdates());

        notificationPreferenceJpaRepository.save(entity);
        return toNotificationResponse(entity);
    }

    private UserJpaEntity getUser(UUID userId) {
        return userJpaRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GEN_003, "Usuario nao encontrado"));
    }

    private NotificationPreferenceJpaEntity getOrCreateNotificationPreferences(UUID userId) {
        getUser(userId);

        return notificationPreferenceJpaRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationPreferenceJpaEntity entity = new NotificationPreferenceJpaEntity();
                    entity.setId(UUID.randomUUID());
                    entity.setUserId(userId);
                    return notificationPreferenceJpaRepository.save(entity);
                });
    }

    private NotificationPreferencesResponse toNotificationResponse(NotificationPreferenceJpaEntity entity) {
        return new NotificationPreferencesResponse(
                entity.isEmailEnabled(),
                entity.isPushEnabled(),
                entity.isBillingAlerts(),
                entity.isUsageAlerts(),
                entity.isSecurityAlerts(),
                entity.isProductUpdates()
        );
    }

    private ObjectNode parseMetadata(String rawMetadata) {
        if (rawMetadata == null || rawMetadata.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            JsonNode node = objectMapper.readTree(rawMetadata);
            if (node instanceof ObjectNode objectNode) {
                return objectNode.deepCopy();
            }
        } catch (Exception ignored) {
            // fallback below
        }

        return objectMapper.createObjectNode();
    }

    private String writeMetadata(ObjectNode metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao serializar metadata do usuario", exception);
        }
    }
}
