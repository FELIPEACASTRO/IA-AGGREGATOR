-- ============================================================================
-- IA AGGREGATOR - Module 1: AUTH
-- File: 01_auth.sql
-- Purpose: Users, organizations, roles, sessions, API keys, LGPD compliance
-- ============================================================================

SET search_path TO auth, public;

-- ============================================================================
-- ORGANIZATIONS
-- ============================================================================

CREATE TABLE auth.organizations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    org_type        org_type NOT NULL DEFAULT 'personal',
    logo_url        TEXT,
    -- Billing link
    stripe_customer_id VARCHAR(255),
    -- Settings stored as JSONB for flexibility
    settings        JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- LGPD: data processing basis
    data_processing_basis TEXT,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ, -- soft delete

    CONSTRAINT org_slug_format CHECK (slug ~ '^[a-z0-9][a-z0-9-]*[a-z0-9]$')
);

COMMENT ON TABLE auth.organizations IS 'Top-level organizational entity for multi-tenancy. Personal orgs are created automatically for each user.';
COMMENT ON COLUMN auth.organizations.settings IS 'JSON config: default_model, allowed_models, feature_flags, branding, etc.';
COMMENT ON COLUMN auth.organizations.data_processing_basis IS 'LGPD legal basis for data processing (consent, contract, legitimate_interest).';

CREATE INDEX idx_organizations_slug ON auth.organizations(slug);
CREATE INDEX idx_organizations_type ON auth.organizations(org_type);
CREATE INDEX idx_organizations_deleted_at ON auth.organizations(deleted_at) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON auth.organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- USERS
-- ============================================================================

CREATE TABLE auth.users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Personal org (1:1 relationship)
    personal_org_id UUID REFERENCES auth.organizations(id),
    -- Identity
    email           VARCHAR(320) NOT NULL UNIQUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    password_hash   TEXT, -- NULL when using OAuth only
    -- Profile
    full_name       VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    avatar_url      TEXT,
    phone           VARCHAR(20),
    phone_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    -- Locale (Brazilian market)
    locale          VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    timezone        VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    -- Platform role
    role            user_role NOT NULL DEFAULT 'user',
    status          user_status NOT NULL DEFAULT 'pending_verification',
    -- Auth metadata
    last_login_at   TIMESTAMPTZ,
    last_login_ip   INET,
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    -- LGPD fields
    cpf_hash        TEXT, -- Hashed CPF for uniqueness check without storing PII
    data_export_requested_at TIMESTAMPTZ,
    -- Onboarding
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    onboarding_step INTEGER NOT NULL DEFAULT 0,
    -- Referral
    referred_by_user_id UUID REFERENCES auth.users(id),
    referral_code   VARCHAR(20) UNIQUE,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ -- soft delete for LGPD erasure flow
);

COMMENT ON TABLE auth.users IS 'Core user accounts. Each user gets a personal organization. Supports email + OAuth.';
COMMENT ON COLUMN auth.users.password_hash IS 'bcrypt/argon2 hash. NULL if user only uses OAuth providers.';
COMMENT ON COLUMN auth.users.cpf_hash IS 'SHA-256 hash of CPF for uniqueness verification without storing raw PII (LGPD compliance).';
COMMENT ON COLUMN auth.users.metadata IS 'Flexible JSON: preferred_model, theme, notification_preferences, feature_flags, etc.';
COMMENT ON COLUMN auth.users.referral_code IS 'Unique code for user-to-user referral (distinct from partner system).';

CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_users_status ON auth.users(status);
CREATE INDEX idx_users_role ON auth.users(role);
CREATE INDEX idx_users_personal_org ON auth.users(personal_org_id);
CREATE INDEX idx_users_referral_code ON auth.users(referral_code) WHERE referral_code IS NOT NULL;
CREATE INDEX idx_users_created_at ON auth.users(created_at);
CREATE INDEX idx_users_deleted_at ON auth.users(deleted_at) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- USER OAUTH IDENTITIES (multiple providers per user)
-- ============================================================================

CREATE TABLE auth.user_identities (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    provider        auth_provider NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email  VARCHAR(320),
    provider_data   JSONB NOT NULL DEFAULT '{}'::jsonb, -- profile data from provider
    access_token_enc TEXT, -- encrypted OAuth access token
    refresh_token_enc TEXT, -- encrypted OAuth refresh token
    token_expires_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_identity UNIQUE (provider, provider_user_id)
);

COMMENT ON TABLE auth.user_identities IS 'OAuth/SSO provider identities linked to a user. One user can have multiple providers.';

CREATE INDEX idx_user_identities_user ON auth.user_identities(user_id);
CREATE INDEX idx_user_identities_provider ON auth.user_identities(provider, provider_user_id);

CREATE TRIGGER trg_user_identities_updated_at
    BEFORE UPDATE ON auth.user_identities
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- SESSIONS
-- ============================================================================

CREATE TABLE auth.sessions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    -- Token management
    token_hash      TEXT NOT NULL UNIQUE, -- SHA-256 of session token
    refresh_token_hash TEXT UNIQUE, -- SHA-256 of refresh token
    -- Session metadata
    status          session_status NOT NULL DEFAULT 'active',
    ip_address      INET,
    user_agent      TEXT,
    device_info     JSONB, -- parsed user agent: os, browser, device type
    -- Expiration
    expires_at      TIMESTAMPTZ NOT NULL,
    refresh_expires_at TIMESTAMPTZ,
    last_active_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Revocation
    revoked_at      TIMESTAMPTZ,
    revoked_reason  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auth.sessions IS 'Active user sessions with JWT token tracking. Supports refresh token rotation.';
COMMENT ON COLUMN auth.sessions.token_hash IS 'SHA-256 hash of the JWT access token for validation without storing raw tokens.';

CREATE INDEX idx_sessions_user ON auth.sessions(user_id);
CREATE INDEX idx_sessions_token ON auth.sessions(token_hash);
CREATE INDEX idx_sessions_status ON auth.sessions(status) WHERE status = 'active';
CREATE INDEX idx_sessions_expires ON auth.sessions(expires_at);

-- ============================================================================
-- API KEYS
-- ============================================================================

CREATE TABLE auth.api_keys (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    -- Key data
    name            VARCHAR(100) NOT NULL,
    key_prefix      VARCHAR(8) NOT NULL, -- e.g., "ia-ag_" visible prefix for identification
    key_hash        TEXT NOT NULL UNIQUE, -- SHA-256 of full API key
    -- Permissions
    scopes          TEXT[] NOT NULL DEFAULT ARRAY['chat:read', 'chat:write'], -- permission scopes
    -- Limits
    rate_limit_rpm  INTEGER, -- requests per minute override
    monthly_credit_limit BIGINT, -- optional credit limit for this key
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at    TIMESTAMPTZ,
    last_used_ip    INET,
    expires_at      TIMESTAMPTZ,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,

    CONSTRAINT api_key_name_length CHECK (char_length(name) >= 1)
);

COMMENT ON TABLE auth.api_keys IS 'API keys for programmatic access. Full key shown only once at creation; only hash stored.';
COMMENT ON COLUMN auth.api_keys.scopes IS 'Array of permission scopes: chat:read, chat:write, models:list, usage:read, billing:read, etc.';
COMMENT ON COLUMN auth.api_keys.key_prefix IS 'First 8 chars of the key displayed in UI for identification (e.g., ia-ag_Xk...).';

CREATE INDEX idx_api_keys_user ON auth.api_keys(user_id);
CREATE INDEX idx_api_keys_org ON auth.api_keys(org_id);
CREATE INDEX idx_api_keys_hash ON auth.api_keys(key_hash);
CREATE INDEX idx_api_keys_active ON auth.api_keys(is_active) WHERE is_active = TRUE;

-- ============================================================================
-- LGPD: CONSENT RECORDS
-- ============================================================================

CREATE TABLE auth.consent_records (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    consent_type    consent_type NOT NULL,
    -- Consent details
    version         VARCHAR(20) NOT NULL, -- version of the terms/policy
    granted         BOOLEAN NOT NULL,
    granted_at      TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    -- Proof
    ip_address      INET,
    user_agent      TEXT,
    -- The document the user consented to
    document_url    TEXT,
    document_hash   TEXT, -- SHA-256 of the document content at time of consent
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_consent_version UNIQUE (user_id, consent_type, version)
);

COMMENT ON TABLE auth.consent_records IS 'LGPD compliance: immutable log of user consent grants and revocations. Never updated, only new records inserted.';

CREATE INDEX idx_consent_user ON auth.consent_records(user_id);
CREATE INDEX idx_consent_type ON auth.consent_records(consent_type);

-- ============================================================================
-- LGPD: DATA ERASURE REQUESTS
-- ============================================================================

CREATE TABLE auth.erasure_requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    -- Request details
    status          erasure_status NOT NULL DEFAULT 'requested',
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Processing
    processed_at    TIMESTAMPTZ,
    processed_by    UUID REFERENCES auth.users(id), -- admin who processed
    -- What was erased
    erased_tables   TEXT[], -- list of tables where data was erased
    data_retained   TEXT[], -- any data retained (legal basis) and why
    retention_reason TEXT, -- legal basis for any retained data
    -- Audit
    notes           TEXT,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auth.erasure_requests IS 'LGPD Art. 18: Right to erasure requests. Tracks full lifecycle of data deletion.';

CREATE INDEX idx_erasure_user ON auth.erasure_requests(user_id);
CREATE INDEX idx_erasure_status ON auth.erasure_requests(status);

-- ============================================================================
-- PASSWORD RESET TOKENS
-- ============================================================================

CREATE TABLE auth.password_reset_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash      TEXT NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    used_at         TIMESTAMPTZ,
    ip_address      INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auth.password_reset_tokens IS 'Time-limited password reset tokens. Token is hashed; raw value sent via email.';

CREATE INDEX idx_pwd_reset_user ON auth.password_reset_tokens(user_id);
CREATE INDEX idx_pwd_reset_token ON auth.password_reset_tokens(token_hash);
CREATE INDEX idx_pwd_reset_expires ON auth.password_reset_tokens(expires_at);

-- ============================================================================
-- EMAIL VERIFICATION TOKENS
-- ============================================================================

CREATE TABLE auth.email_verification_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email           VARCHAR(320) NOT NULL,
    token_hash      TEXT NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auth.email_verification_tokens IS 'Email verification tokens sent during signup or email change.';

CREATE INDEX idx_email_verify_user ON auth.email_verification_tokens(user_id);
CREATE INDEX idx_email_verify_token ON auth.email_verification_tokens(token_hash);

-- ============================================================================
-- USER NOTIFICATION PREFERENCES
-- ============================================================================

CREATE TABLE auth.notification_preferences (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    -- Channels
    email_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    -- Categories
    billing_alerts  BOOLEAN NOT NULL DEFAULT TRUE,
    usage_alerts    BOOLEAN NOT NULL DEFAULT TRUE,
    credit_low_threshold INTEGER DEFAULT 100, -- alert when credits below this
    product_updates BOOLEAN NOT NULL DEFAULT TRUE,
    marketing       BOOLEAN NOT NULL DEFAULT FALSE,
    security_alerts BOOLEAN NOT NULL DEFAULT TRUE, -- cannot be disabled
    partner_updates BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_notification_user UNIQUE (user_id)
);

COMMENT ON TABLE auth.notification_preferences IS 'Per-user notification channel and category preferences.';

CREATE TRIGGER trg_notification_prefs_updated_at
    BEFORE UPDATE ON auth.notification_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- FEATURE FLAGS (per-user overrides)
-- ============================================================================

CREATE TABLE auth.feature_flags (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    is_enabled      BOOLEAN NOT NULL DEFAULT FALSE, -- global default
    -- Targeting
    enabled_for_users UUID[], -- specific user IDs
    enabled_for_orgs UUID[], -- specific org IDs
    enabled_for_tiers plan_tier[], -- specific plan tiers
    rollout_percentage SMALLINT DEFAULT 0 CHECK (rollout_percentage BETWEEN 0 AND 100),
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auth.feature_flags IS 'Feature flags for gradual rollout. Supports user, org, and plan-tier targeting.';

CREATE INDEX idx_feature_flags_name ON auth.feature_flags(name);

CREATE TRIGGER trg_feature_flags_updated_at
    BEFORE UPDATE ON auth.feature_flags
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
