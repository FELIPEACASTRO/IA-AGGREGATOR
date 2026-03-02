-- ============================================================================
-- IA AGGREGATOR - Module 4: AI GATEWAY
-- File: 04_ai_gateway.sql
-- Purpose: AI providers, models, routing rules, cache, rate limits, requests
-- ============================================================================

SET search_path TO ai_gateway, public;

-- ============================================================================
-- PROVIDERS
-- ============================================================================

CREATE TABLE ai_gateway.providers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Identity
    name            VARCHAR(100) NOT NULL UNIQUE, -- 'OpenAI', 'Anthropic', 'Google', etc.
    slug            VARCHAR(50) NOT NULL UNIQUE,
    logo_url        TEXT,
    website_url     TEXT,
    -- API configuration
    base_url        TEXT NOT NULL,
    api_version     VARCHAR(20),
    auth_header     VARCHAR(50) NOT NULL DEFAULT 'Authorization',
    auth_prefix     VARCHAR(20) NOT NULL DEFAULT 'Bearer',
    -- Encrypted API key (master key for this provider)
    api_key_enc     TEXT NOT NULL,
    -- Status
    status          provider_status NOT NULL DEFAULT 'active',
    -- Health check
    health_check_url TEXT,
    last_health_check_at TIMESTAMPTZ,
    health_check_status provider_status,
    uptime_percentage NUMERIC(5,2) DEFAULT 100.0,
    -- Rate limits from provider
    global_rpm_limit INTEGER, -- requests per minute
    global_tpm_limit BIGINT, -- tokens per minute
    -- Cost tracking
    cost_per_1k_input_tokens NUMERIC(10,6), -- provider's cost in USD
    cost_per_1k_output_tokens NUMERIC(10,6),
    -- Metadata
    supported_features TEXT[], -- ['streaming', 'function_calling', 'vision', 'json_mode']
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_gateway.providers IS 'AI provider configurations (OpenAI, Anthropic, Google, Mistral, etc.). Stores encrypted API keys.';
COMMENT ON COLUMN ai_gateway.providers.api_key_enc IS 'AES-256 encrypted master API key for this provider.';

CREATE INDEX idx_providers_slug ON ai_gateway.providers(slug);
CREATE INDEX idx_providers_status ON ai_gateway.providers(status);

CREATE TRIGGER trg_providers_updated_at
    BEFORE UPDATE ON ai_gateway.providers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- MODELS
-- ============================================================================

CREATE TABLE ai_gateway.models (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id     UUID NOT NULL REFERENCES ai_gateway.providers(id),
    -- Identity
    name            VARCHAR(200) NOT NULL, -- display name
    slug            VARCHAR(100) NOT NULL UNIQUE, -- internal identifier
    provider_model_id VARCHAR(200) NOT NULL, -- model ID as known by the provider API
    -- Category
    category        model_category NOT NULL DEFAULT 'chat',
    -- Capabilities
    max_context_tokens INTEGER NOT NULL DEFAULT 4096,
    max_output_tokens INTEGER,
    supports_streaming BOOLEAN NOT NULL DEFAULT TRUE,
    supports_function_calling BOOLEAN NOT NULL DEFAULT FALSE,
    supports_vision BOOLEAN NOT NULL DEFAULT FALSE,
    supports_json_mode BOOLEAN NOT NULL DEFAULT FALSE,
    supports_system_prompt BOOLEAN NOT NULL DEFAULT TRUE,
    -- Credit pricing
    credit_multiplier NUMERIC(6,4) NOT NULL DEFAULT 1.0, -- credits per standard unit
    credit_per_input_token NUMERIC(10,8) NOT NULL DEFAULT 0.001,
    credit_per_output_token NUMERIC(10,8) NOT NULL DEFAULT 0.002,
    -- Model tier (determines plan access)
    model_tier      VARCHAR(20) NOT NULL DEFAULT 'standard', -- basic, standard, premium, enterprise
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_visible      BOOLEAN NOT NULL DEFAULT TRUE, -- show in model picker
    is_default      BOOLEAN NOT NULL DEFAULT FALSE, -- default model for new conversations
    -- Performance benchmarks
    avg_latency_ms  INTEGER, -- average response latency
    quality_score   NUMERIC(3,2), -- 0.00 to 1.00 quality rating
    -- Display
    description     TEXT,
    short_description VARCHAR(200),
    icon_url        TEXT,
    color           VARCHAR(7), -- hex color for UI
    sort_order      INTEGER DEFAULT 0,
    -- Deprecation
    deprecated_at   TIMESTAMPTZ,
    replacement_model_id UUID REFERENCES ai_gateway.models(id),
    sunset_date     DATE,
    -- Metadata
    capabilities    JSONB NOT NULL DEFAULT '{}'::jsonb, -- detailed capability flags
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_gateway.models IS 'Available AI models with pricing, capabilities, and tier access control.';
COMMENT ON COLUMN ai_gateway.models.credit_multiplier IS 'Multiplier applied to base credit cost. GPT-4=3.0x, GPT-3.5=1.0x, Claude-3-Opus=4.0x, etc.';
COMMENT ON COLUMN ai_gateway.models.model_tier IS 'Access tier: basic (free), standard (starter+), premium (pro+), enterprise.';

CREATE INDEX idx_models_provider ON ai_gateway.models(provider_id);
CREATE INDEX idx_models_slug ON ai_gateway.models(slug);
CREATE INDEX idx_models_category ON ai_gateway.models(category);
CREATE INDEX idx_models_tier ON ai_gateway.models(model_tier);
CREATE INDEX idx_models_active ON ai_gateway.models(is_active, is_visible);
CREATE INDEX idx_models_default ON ai_gateway.models(is_default) WHERE is_default = TRUE;

CREATE TRIGGER trg_models_updated_at
    BEFORE UPDATE ON ai_gateway.models
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Now add FK from chat.messages and chat.conversations to models
ALTER TABLE chat.messages
    ADD CONSTRAINT fk_messages_model
    FOREIGN KEY (model_id) REFERENCES ai_gateway.models(id);

ALTER TABLE chat.conversations
    ADD CONSTRAINT fk_conversations_model
    FOREIGN KEY (model_id) REFERENCES ai_gateway.models(id);

-- Also add FK from billing.credit_transactions to models
ALTER TABLE billing.credit_transactions
    ADD CONSTRAINT fk_credit_tx_model
    FOREIGN KEY (model_id) REFERENCES ai_gateway.models(id);

-- ============================================================================
-- MODEL PRICING HISTORY (track price changes over time)
-- ============================================================================

CREATE TABLE ai_gateway.model_pricing_history (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_id        UUID NOT NULL REFERENCES ai_gateway.models(id) ON DELETE CASCADE,
    credit_multiplier NUMERIC(6,4) NOT NULL,
    credit_per_input_token NUMERIC(10,8) NOT NULL,
    credit_per_output_token NUMERIC(10,8) NOT NULL,
    effective_from  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_until TIMESTAMPTZ,
    changed_by      UUID REFERENCES auth.users(id),
    reason          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_gateway.model_pricing_history IS 'Historical record of model credit pricing changes for audit trail.';

CREATE INDEX idx_pricing_history_model ON ai_gateway.model_pricing_history(model_id);
CREATE INDEX idx_pricing_history_effective ON ai_gateway.model_pricing_history(effective_from);

-- ============================================================================
-- ROUTING RULES
-- ============================================================================

CREATE TABLE ai_gateway.routing_rules (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Identity
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    -- Targeting
    org_id          UUID REFERENCES auth.organizations(id), -- NULL = global rule
    plan_tier       plan_tier, -- NULL = all tiers
    -- Strategy
    strategy        routing_strategy NOT NULL DEFAULT 'lowest_cost',
    -- Primary model
    primary_model_id UUID NOT NULL REFERENCES ai_gateway.models(id),
    -- Fallback models (ordered)
    fallback_model_ids UUID[] DEFAULT ARRAY[]::UUID[],
    -- Conditions (JSONB for flexibility)
    conditions      JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- e.g., {"max_tokens": 8000, "time_range": "09:00-17:00", "load_threshold": 0.8}
    -- Priority (higher = evaluated first)
    priority        INTEGER NOT NULL DEFAULT 0,
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_gateway.routing_rules IS 'Dynamic routing rules for model selection: fallback chains, load balancing, cost optimization.';
COMMENT ON COLUMN ai_gateway.routing_rules.conditions IS 'JSON conditions: max_tokens, time_range, region, load_threshold, category, etc.';

CREATE INDEX idx_routing_rules_org ON ai_gateway.routing_rules(org_id);
CREATE INDEX idx_routing_rules_priority ON ai_gateway.routing_rules(priority DESC);
CREATE INDEX idx_routing_rules_active ON ai_gateway.routing_rules(is_active) WHERE is_active = TRUE;

CREATE TRIGGER trg_routing_rules_updated_at
    BEFORE UPDATE ON ai_gateway.routing_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- AI REQUESTS LOG (detailed request/response tracking)
-- ============================================================================

CREATE TABLE ai_gateway.requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Ownership
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- What was called
    provider_id     UUID NOT NULL REFERENCES ai_gateway.providers(id),
    model_id        UUID NOT NULL REFERENCES ai_gateway.models(id),
    -- Linked entities
    message_id      UUID, -- FK to chat.messages
    api_key_id      UUID REFERENCES auth.api_keys(id), -- if via API key
    conversation_id UUID, -- FK to chat.conversations
    -- Request details
    request_type    model_category NOT NULL DEFAULT 'chat',
    -- Token usage
    tokens_input    INTEGER NOT NULL DEFAULT 0,
    tokens_output   INTEGER NOT NULL DEFAULT 0,
    tokens_total    INTEGER NOT NULL DEFAULT 0,
    -- Credit cost
    credits_used    BIGINT NOT NULL DEFAULT 0,
    credit_multiplier NUMERIC(6,4) DEFAULT 1.0,
    -- Performance
    latency_ms      INTEGER, -- total latency
    time_to_first_token_ms INTEGER, -- streaming: time to first token
    -- Cache
    cache_status    cache_status,
    cache_key_hash  TEXT,
    -- Routing
    routing_rule_id UUID REFERENCES ai_gateway.routing_rules(id),
    was_fallback    BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_reason TEXT,
    -- Status
    status_code     SMALLINT, -- HTTP status code from provider
    is_success      BOOLEAN NOT NULL DEFAULT TRUE,
    error_code      VARCHAR(100),
    error_message   TEXT,
    -- Request metadata
    ip_address      INET,
    user_agent      TEXT,
    -- Streaming
    is_streaming    BOOLEAN NOT NULL DEFAULT FALSE,
    stream_chunks   INTEGER,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_gateway.requests IS 'Detailed log of every AI API request. Used for billing, analytics, debugging, and rate limiting.';
COMMENT ON COLUMN ai_gateway.requests.cache_status IS 'Whether this request was served from cache (hit) or sent to provider (miss).';

-- This table will be large; optimized indexes for common queries
CREATE INDEX idx_requests_user ON ai_gateway.requests(user_id);
CREATE INDEX idx_requests_org ON ai_gateway.requests(org_id);
CREATE INDEX idx_requests_model ON ai_gateway.requests(model_id);
CREATE INDEX idx_requests_provider ON ai_gateway.requests(provider_id);
CREATE INDEX idx_requests_created ON ai_gateway.requests(created_at);
CREATE INDEX idx_requests_org_created ON ai_gateway.requests(org_id, created_at DESC);
CREATE INDEX idx_requests_user_created ON ai_gateway.requests(user_id, created_at DESC);
CREATE INDEX idx_requests_success ON ai_gateway.requests(is_success) WHERE is_success = FALSE;
CREATE INDEX idx_requests_cache ON ai_gateway.requests(cache_status);
CREATE INDEX idx_requests_conversation ON ai_gateway.requests(conversation_id) WHERE conversation_id IS NOT NULL;

-- ============================================================================
-- RESPONSE CACHE
-- ============================================================================

CREATE TABLE ai_gateway.response_cache (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Cache key (hash of model + messages + params)
    cache_key_hash  TEXT NOT NULL UNIQUE,
    -- What was cached
    model_id        UUID NOT NULL REFERENCES ai_gateway.models(id),
    -- Cached response
    response_content TEXT NOT NULL,
    response_tokens INTEGER NOT NULL,
    -- Metadata about the original request
    original_request_id UUID REFERENCES ai_gateway.requests(id),
    -- Cache management
    hit_count       INTEGER NOT NULL DEFAULT 0,
    last_hit_at     TIMESTAMPTZ,
    -- TTL
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_gateway.response_cache IS 'Semantic response cache to reduce API costs and latency for identical/similar requests.';

CREATE INDEX idx_response_cache_key ON ai_gateway.response_cache(cache_key_hash);
CREATE INDEX idx_response_cache_expires ON ai_gateway.response_cache(expires_at);
CREATE INDEX idx_response_cache_model ON ai_gateway.response_cache(model_id);

-- ============================================================================
-- RATE LIMITS
-- ============================================================================

CREATE TABLE ai_gateway.rate_limits (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Scope (one of these should be set)
    plan_tier       plan_tier, -- rate limit per plan tier
    org_id          UUID REFERENCES auth.organizations(id), -- custom org override
    user_id         UUID REFERENCES auth.users(id), -- custom user override
    api_key_id      UUID REFERENCES auth.api_keys(id), -- per API key
    -- Limits
    window          rate_limit_window NOT NULL DEFAULT 'minute',
    max_requests    INTEGER NOT NULL,
    max_tokens      BIGINT, -- optional token limit per window
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT rate_limit_has_scope CHECK (
        plan_tier IS NOT NULL OR org_id IS NOT NULL OR user_id IS NOT NULL OR api_key_id IS NOT NULL
    )
);

COMMENT ON TABLE ai_gateway.rate_limits IS 'Rate limit configurations. Can be scoped to plan tier, org, user, or API key.';

CREATE INDEX idx_rate_limits_tier ON ai_gateway.rate_limits(plan_tier);
CREATE INDEX idx_rate_limits_org ON ai_gateway.rate_limits(org_id);
CREATE INDEX idx_rate_limits_user ON ai_gateway.rate_limits(user_id);
CREATE INDEX idx_rate_limits_api_key ON ai_gateway.rate_limits(api_key_id);

CREATE TRIGGER trg_rate_limits_updated_at
    BEFORE UPDATE ON ai_gateway.rate_limits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- RATE LIMIT COUNTERS (ephemeral, could also be Redis-backed)
-- ============================================================================

CREATE TABLE ai_gateway.rate_limit_counters (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Identifier
    identifier      VARCHAR(255) NOT NULL, -- user_id, org_id, api_key_id, or IP
    identifier_type VARCHAR(20) NOT NULL, -- 'user', 'org', 'api_key', 'ip'
    window          rate_limit_window NOT NULL,
    -- Counters
    request_count   INTEGER NOT NULL DEFAULT 0,
    token_count     BIGINT NOT NULL DEFAULT 0,
    -- Window boundaries
    window_start    TIMESTAMPTZ NOT NULL,
    window_end      TIMESTAMPTZ NOT NULL,
    -- Metadata
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_rate_counter UNIQUE (identifier, identifier_type, window, window_start)
);

COMMENT ON TABLE ai_gateway.rate_limit_counters IS 'Current rate limit window counters. Can be replaced by Redis in production.';

CREATE INDEX idx_rate_counters_identifier ON ai_gateway.rate_limit_counters(identifier, identifier_type);
CREATE INDEX idx_rate_counters_window ON ai_gateway.rate_limit_counters(window_end);

-- ============================================================================
-- PROVIDER HEALTH LOG
-- ============================================================================

CREATE TABLE ai_gateway.provider_health_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id     UUID NOT NULL REFERENCES ai_gateway.providers(id),
    -- Health check result
    status          provider_status NOT NULL,
    response_time_ms INTEGER,
    status_code     SMALLINT,
    error_message   TEXT,
    -- Metadata
    checked_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ai_gateway.provider_health_log IS 'Health check results for each provider. Used for routing decisions and SLA tracking.';

CREATE INDEX idx_health_log_provider ON ai_gateway.provider_health_log(provider_id, checked_at DESC);
