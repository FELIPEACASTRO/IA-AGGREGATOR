package com.ia.aggregator.domain.auth.vo;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Role-to-permissions mapping matrix.
 *
 * <p>Defines the default permission set for each role.
 * Custom overrides can be applied at the organization level.
 */
public final class RolePermissions {

    private RolePermissions() {}

    private static final Map<UserRole, Set<Permission>> MATRIX = Map.of(
            UserRole.SUPER_ADMIN, EnumSet.allOf(Permission.class),

            UserRole.ADMIN, EnumSet.of(
                    Permission.CHAT_CREATE, Permission.CHAT_READ, Permission.CHAT_STREAM,
                    Permission.AI_IMAGE_GENERATE, Permission.AI_IMAGE_EDIT,
                    Permission.AI_VIDEO_GENERATE, Permission.AI_EMBEDDING_CREATE,
                    Permission.AI_SPEECH_TO_TEXT, Permission.AI_TEXT_TO_SPEECH,
                    Permission.AI_OCR, Permission.AI_RERANK,
                    Permission.AI_WEB_SEARCH, Permission.AI_GROUNDED_CHAT,
                    Permission.AI_THREAT_INTEL, Permission.AI_TRANSLATION,
                    Permission.AI_DOCUMENT_PARSE, Permission.AI_3D_GENERATE,
                    Permission.AI_AVATAR_VIDEO, Permission.AI_RESPONSES,
                    Permission.PROVIDER_CATALOG_READ, Permission.PROVIDER_HEALTH_READ,
                    Permission.PROVIDER_CONFIG_MANAGE,
                    Permission.GATEWAY_ROUTING_INSPECT, Permission.GATEWAY_ROUTING_MANAGE,
                    Permission.GATEWAY_CACHE_MANAGE,
                    Permission.TEAM_CREATE, Permission.TEAM_READ, Permission.TEAM_UPDATE,
                    Permission.TEAM_DELETE, Permission.TEAM_MEMBER_MANAGE,
                    Permission.ORG_READ, Permission.ORG_UPDATE,
                    Permission.ORG_BILLING_READ, Permission.ORG_BILLING_MANAGE,
                    Permission.USER_READ, Permission.USER_UPDATE, Permission.USER_ROLE_MANAGE,
                    Permission.API_KEY_CREATE, Permission.API_KEY_READ, Permission.API_KEY_REVOKE,
                    Permission.AUDIT_READ
            ),

            UserRole.USER, EnumSet.of(
                    Permission.CHAT_CREATE, Permission.CHAT_READ, Permission.CHAT_STREAM,
                    Permission.AI_IMAGE_GENERATE, Permission.AI_EMBEDDING_CREATE,
                    Permission.AI_SPEECH_TO_TEXT, Permission.AI_TEXT_TO_SPEECH,
                    Permission.AI_OCR, Permission.AI_WEB_SEARCH,
                    Permission.AI_GROUNDED_CHAT, Permission.AI_TRANSLATION,
                    Permission.AI_DOCUMENT_PARSE, Permission.AI_RESPONSES,
                    Permission.PROVIDER_CATALOG_READ,
                    Permission.TEAM_READ,
                    Permission.ORG_READ, Permission.ORG_BILLING_READ,
                    Permission.USER_READ, Permission.USER_UPDATE,
                    Permission.API_KEY_CREATE, Permission.API_KEY_READ, Permission.API_KEY_REVOKE
            ),

            UserRole.VIEWER, EnumSet.of(
                    Permission.CHAT_READ,
                    Permission.PROVIDER_CATALOG_READ,
                    Permission.TEAM_READ,
                    Permission.ORG_READ,
                    Permission.USER_READ
            ),

            UserRole.API_ONLY, EnumSet.of(
                    Permission.CHAT_CREATE, Permission.CHAT_READ, Permission.CHAT_STREAM,
                    Permission.AI_IMAGE_GENERATE, Permission.AI_EMBEDDING_CREATE,
                    Permission.AI_SPEECH_TO_TEXT, Permission.AI_TEXT_TO_SPEECH,
                    Permission.AI_OCR, Permission.AI_RERANK,
                    Permission.AI_WEB_SEARCH, Permission.AI_GROUNDED_CHAT,
                    Permission.AI_TRANSLATION, Permission.AI_DOCUMENT_PARSE,
                    Permission.AI_RESPONSES,
                    Permission.PROVIDER_CATALOG_READ
            )
    );

    /**
     * Get permissions for a role.
     */
    public static Set<Permission> forRole(UserRole role) {
        return MATRIX.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }

    /**
     * Check if a role has a specific permission.
     */
    public static boolean hasPermission(UserRole role, Permission permission) {
        return forRole(role).contains(permission);
    }
}
