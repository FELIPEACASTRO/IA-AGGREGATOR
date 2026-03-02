-- ============================================================================
-- V2: Auth Module Tables
-- ============================================================================

SET search_path TO auth, public;

-- Organizations
CREATE TABLE auth.organizations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    org_type        org_type NOT NULL DEFAULT 'personal',
    logo_url        TEXT,
    stripe_customer_id VARCHAR(255),
    settings        JSONB NOT NULL DEFAULT '{}'::jsonb,
    data_processing_basis TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT org_slug_format CHECK (slug ~ '^[a-z0-9][a-z0-9-]*[a-z0-9]$')
);

CREATE INDEX idx_organizations_slug ON auth.organizations(slug);
CREATE INDEX idx_organizations_type ON auth.organizations(org_type);
CREATE INDEX idx_organizations_deleted_at ON auth.organizations(deleted_at) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON auth.organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Users
CREATE TABLE auth.users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    personal_org_id UUID REFERENCES auth.organizations(id),
    email           VARCHAR(320) NOT NULL UNIQUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified_at TIMESTAMPTZ,
    password_hash   TEXT,
    full_name       VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    avatar_url      TEXT,
    phone           VARCHAR(20),
    phone_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    locale          VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    timezone        VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    role            user_role NOT NULL DEFAULT 'user',
    status          user_status NOT NULL DEFAULT 'pending_verification',
    last_login_at   TIMESTAMPTZ,
    last_login_ip   INET,
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    cpf_hash        TEXT,
    data_export_requested_at TIMESTAMPTZ,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    onboarding_step INTEGER NOT NULL DEFAULT 0,
    referred_by_user_id UUID REFERENCES auth.users(id),
    referral_code   VARCHAR(20) UNIQUE,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

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

-- OAuth Identities
CREATE TABLE auth.user_identities (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    provider        auth_provider NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email  VARCHAR(320),
    provider_data   JSONB NOT NULL DEFAULT '{}'::jsonb,
    access_token_enc TEXT,
    refresh_token_enc TEXT,
    token_expires_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_identity UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_user_identities_user ON auth.user_identities(user_id);
CREATE INDEX idx_user_identities_provider ON auth.user_identities(provider, provider_user_id);

CREATE TRIGGER trg_user_identities_updated_at
    BEFORE UPDATE ON auth.user_identities
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Sessions
CREATE TABLE auth.sessions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash      TEXT NOT NULL UNIQUE,
    refresh_token_hash TEXT UNIQUE,
    status          session_status NOT NULL DEFAULT 'active',
    ip_address      INET,
    user_agent      TEXT,
    device_info     JSONB,
    expires_at      TIMESTAMPTZ NOT NULL,
    refresh_expires_at TIMESTAMPTZ,
    last_active_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    revoked_reason  TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user ON auth.sessions(user_id);
CREATE INDEX idx_sessions_token ON auth.sessions(token_hash);
CREATE INDEX idx_sessions_status ON auth.sessions(status) WHERE status = 'active';
CREATE INDEX idx_sessions_expires ON auth.sessions(expires_at);

-- API Keys
CREATE TABLE auth.api_keys (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    key_prefix      VARCHAR(8) NOT NULL,
    key_hash        TEXT NOT NULL UNIQUE,
    scopes          TEXT[] NOT NULL DEFAULT ARRAY['chat:read', 'chat:write'],
    rate_limit_rpm  INTEGER,
    monthly_credit_limit BIGINT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at    TIMESTAMPTZ,
    last_used_ip    INET,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    CONSTRAINT api_key_name_length CHECK (char_length(name) >= 1)
);

CREATE INDEX idx_api_keys_user ON auth.api_keys(user_id);
CREATE INDEX idx_api_keys_org ON auth.api_keys(org_id);
CREATE INDEX idx_api_keys_hash ON auth.api_keys(key_hash);
CREATE INDEX idx_api_keys_active ON auth.api_keys(is_active) WHERE is_active = TRUE;

-- LGPD Consent Records
CREATE TABLE auth.consent_records (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    consent_type    consent_type NOT NULL,
    version         VARCHAR(20) NOT NULL,
    granted         BOOLEAN NOT NULL,
    granted_at      TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    ip_address      INET,
    user_agent      TEXT,
    document_url    TEXT,
    document_hash   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_consent_version UNIQUE (user_id, consent_type, version)
);

CREATE INDEX idx_consent_user ON auth.consent_records(user_id);
CREATE INDEX idx_consent_type ON auth.consent_records(consent_type);

-- LGPD Erasure Requests
CREATE TABLE auth.erasure_requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    status          erasure_status NOT NULL DEFAULT 'requested',
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,
    processed_by    UUID REFERENCES auth.users(id),
    erased_tables   TEXT[],
    data_retained   TEXT[],
    retention_reason TEXT,
    notes           TEXT,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_erasure_user ON auth.erasure_requests(user_id);
CREATE INDEX idx_erasure_status ON auth.erasure_requests(status);

-- Password Reset Tokens
CREATE TABLE auth.password_reset_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash      TEXT NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    used_at         TIMESTAMPTZ,
    ip_address      INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pwd_reset_user ON auth.password_reset_tokens(user_id);
CREATE INDEX idx_pwd_reset_token ON auth.password_reset_tokens(token_hash);
CREATE INDEX idx_pwd_reset_expires ON auth.password_reset_tokens(expires_at);

-- Email Verification Tokens
CREATE TABLE auth.email_verification_tokens (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email           VARCHAR(320) NOT NULL,
    token_hash      TEXT NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verify_user ON auth.email_verification_tokens(user_id);
CREATE INDEX idx_email_verify_token ON auth.email_verification_tokens(token_hash);

-- Notification Preferences
CREATE TABLE auth.notification_preferences (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    billing_alerts  BOOLEAN NOT NULL DEFAULT TRUE,
    usage_alerts    BOOLEAN NOT NULL DEFAULT TRUE,
    credit_low_threshold INTEGER DEFAULT 100,
    product_updates BOOLEAN NOT NULL DEFAULT TRUE,
    marketing       BOOLEAN NOT NULL DEFAULT FALSE,
    security_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    partner_updates BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_user UNIQUE (user_id)
);

CREATE TRIGGER trg_notification_prefs_updated_at
    BEFORE UPDATE ON auth.notification_preferences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Feature Flags
CREATE TABLE auth.feature_flags (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    is_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    enabled_for_users UUID[],
    enabled_for_orgs UUID[],
    enabled_for_tiers plan_tier[],
    rollout_percentage SMALLINT DEFAULT 0 CHECK (rollout_percentage BETWEEN 0 AND 100),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feature_flags_name ON auth.feature_flags(name);

CREATE TRIGGER trg_feature_flags_updated_at
    BEFORE UPDATE ON auth.feature_flags
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
