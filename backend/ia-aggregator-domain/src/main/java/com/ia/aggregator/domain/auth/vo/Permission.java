package com.ia.aggregator.domain.auth.vo;

/**
 * Fine-grained permissions for RBAC.
 *
 * <p>Organized by resource:action pattern.
 */
public enum Permission {

    // Chat
    CHAT_CREATE("chat:create"),
    CHAT_READ("chat:read"),
    CHAT_STREAM("chat:stream"),

    // AI Capabilities
    AI_IMAGE_GENERATE("ai:image:generate"),
    AI_IMAGE_EDIT("ai:image:edit"),
    AI_VIDEO_GENERATE("ai:video:generate"),
    AI_EMBEDDING_CREATE("ai:embedding:create"),
    AI_SPEECH_TO_TEXT("ai:speech:transcribe"),
    AI_TEXT_TO_SPEECH("ai:speech:synthesize"),
    AI_OCR("ai:ocr:process"),
    AI_RERANK("ai:rerank:execute"),
    AI_WEB_SEARCH("ai:search:web"),
    AI_GROUNDED_CHAT("ai:search:grounded"),
    AI_THREAT_INTEL("ai:search:threat-intel"),
    AI_TRANSLATION("ai:translation:translate"),
    AI_DOCUMENT_PARSE("ai:document:parse"),
    AI_3D_GENERATE("ai:3d:generate"),
    AI_AVATAR_VIDEO("ai:avatar:generate"),
    AI_RESPONSES("ai:responses:create"),

    // Models & Providers
    PROVIDER_CATALOG_READ("provider:catalog:read"),
    PROVIDER_HEALTH_READ("provider:health:read"),
    PROVIDER_CONFIG_MANAGE("provider:config:manage"),

    // Gateway & Routing
    GATEWAY_ROUTING_INSPECT("gateway:routing:inspect"),
    GATEWAY_ROUTING_MANAGE("gateway:routing:manage"),
    GATEWAY_CACHE_MANAGE("gateway:cache:manage"),

    // Team Management
    TEAM_CREATE("team:create"),
    TEAM_READ("team:read"),
    TEAM_UPDATE("team:update"),
    TEAM_DELETE("team:delete"),
    TEAM_MEMBER_MANAGE("team:member:manage"),

    // Organization
    ORG_READ("org:read"),
    ORG_UPDATE("org:update"),
    ORG_BILLING_READ("org:billing:read"),
    ORG_BILLING_MANAGE("org:billing:manage"),

    // User Management
    USER_READ("user:read"),
    USER_UPDATE("user:update"),
    USER_DELETE("user:delete"),
    USER_ROLE_MANAGE("user:role:manage"),

    // API Keys
    API_KEY_CREATE("api-key:create"),
    API_KEY_READ("api-key:read"),
    API_KEY_REVOKE("api-key:revoke"),

    // Audit
    AUDIT_READ("audit:read"),

    // Admin
    ADMIN_FULL("admin:full");

    private final String value;

    Permission(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
