-- ============================================================================
-- V7: AI Gateway Module Tables
-- ============================================================================

SET search_path TO ai_gateway, auth, public;

-- Providers
CREATE TABLE ai_gateway.providers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    display_name    VARCHAR(200) NOT NULL,
    status          provider_status NOT NULL DEFAULT 'active',
    base_url        TEXT,
    capabilities    TEXT[] NOT NULL DEFAULT '{}',
    supported_models TEXT[] NOT NULL DEFAULT '{}',
    auth_type       VARCHAR(50) NOT NULL DEFAULT 'api_key',
    rate_limit_rpm  INTEGER,
    rate_limit_tpm  BIGINT,
    config          JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_health_check TIMESTAMPTZ,
    last_health_status VARCHAR(50),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_providers_name ON ai_gateway.providers(name);
CREATE INDEX idx_providers_status ON ai_gateway.providers(status) WHERE status = 'active';

CREATE TRIGGER trg_providers_updated_at
    BEFORE UPDATE ON ai_gateway.providers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Models
CREATE TABLE ai_gateway.models (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id     UUID NOT NULL REFERENCES ai_gateway.providers(id) ON DELETE CASCADE,
    model_id        VARCHAR(200) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    category        model_category NOT NULL DEFAULT 'chat',
    context_window  INTEGER,
    max_output_tokens INTEGER,
    input_cost_per_1k NUMERIC(12, 8),
    output_cost_per_1k NUMERIC(12, 8),
    supports_streaming BOOLEAN NOT NULL DEFAULT TRUE,
    supports_function_calling BOOLEAN NOT NULL DEFAULT FALSE,
    supports_vision BOOLEAN NOT NULL DEFAULT FALSE,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    tier            VARCHAR(50) DEFAULT 'balanced',
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_provider_model UNIQUE (provider_id, model_id)
);

CREATE INDEX idx_models_provider ON ai_gateway.models(provider_id);
CREATE INDEX idx_models_model_id ON ai_gateway.models(model_id);
CREATE INDEX idx_models_category ON ai_gateway.models(category);
CREATE INDEX idx_models_default ON ai_gateway.models(is_default) WHERE is_default = TRUE;
CREATE INDEX idx_models_active ON ai_gateway.models(is_active) WHERE is_active = TRUE;

CREATE TRIGGER trg_models_updated_at
    BEFORE UPDATE ON ai_gateway.models
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Routing Rules (persistent version of YAML rules)
CREATE TABLE ai_gateway.routing_rules (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL,
    priority        INTEGER NOT NULL DEFAULT 100,
    strategy        routing_strategy NOT NULL DEFAULT 'fallback',
    capabilities    TEXT[] NOT NULL DEFAULT '{}',
    allowed_models  TEXT[] NOT NULL DEFAULT '{}',
    blocked_models  TEXT[] NOT NULL DEFAULT '{}',
    conditions      JSONB NOT NULL DEFAULT '{}'::jsonb,
    provider_weights JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_routing_rules_priority ON ai_gateway.routing_rules(priority);
CREATE INDEX idx_routing_rules_enabled ON ai_gateway.routing_rules(is_enabled) WHERE is_enabled = TRUE;

CREATE TRIGGER trg_routing_rules_updated_at
    BEFORE UPDATE ON ai_gateway.routing_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Provider Metrics (aggregated stats per provider/model)
CREATE TABLE ai_gateway.provider_metrics (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider_id     UUID NOT NULL REFERENCES ai_gateway.providers(id) ON DELETE CASCADE,
    model_id        VARCHAR(200) NOT NULL,
    period          VARCHAR(10) NOT NULL,
    request_count   BIGINT NOT NULL DEFAULT 0,
    success_count   BIGINT NOT NULL DEFAULT 0,
    error_count     BIGINT NOT NULL DEFAULT 0,
    total_tokens    BIGINT NOT NULL DEFAULT 0,
    total_cost_usd  NUMERIC(12, 4) NOT NULL DEFAULT 0,
    avg_latency_ms  INTEGER NOT NULL DEFAULT 0,
    p50_latency_ms  INTEGER NOT NULL DEFAULT 0,
    p95_latency_ms  INTEGER NOT NULL DEFAULT 0,
    p99_latency_ms  INTEGER NOT NULL DEFAULT 0,
    quality_score   NUMERIC(5, 4) NOT NULL DEFAULT 0.5,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_provider_metrics_period UNIQUE (provider_id, model_id, period)
);

CREATE INDEX idx_provider_metrics_provider ON ai_gateway.provider_metrics(provider_id);
CREATE INDEX idx_provider_metrics_period ON ai_gateway.provider_metrics(period);

-- Response Cache
CREATE TABLE ai_gateway.response_cache (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cache_key       VARCHAR(128) NOT NULL UNIQUE,
    prompt_hash     VARCHAR(64) NOT NULL,
    model_id        VARCHAR(200) NOT NULL,
    response_content TEXT NOT NULL,
    status          cache_status NOT NULL DEFAULT 'hit',
    token_count     INTEGER,
    ttl_seconds     INTEGER NOT NULL DEFAULT 300,
    hit_count       INTEGER NOT NULL DEFAULT 0,
    last_hit_at     TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_response_cache_key ON ai_gateway.response_cache(cache_key);
CREATE INDEX idx_response_cache_expires ON ai_gateway.response_cache(expires_at);

-- Seed core providers
INSERT INTO ai_gateway.providers (name, display_name, capabilities, supported_models) VALUES
    ('openai', 'OpenAI', '{CHAT,EMBEDDING,IMAGE_GENERATION,AUDIO_TRANSCRIPTION,AUDIO_TTS}', '{gpt-4o-mini,gpt-4.1-mini,gpt-4o,gpt-4-turbo}'),
    ('anthropic', 'Anthropic', '{CHAT}', '{claude-3-5-haiku,claude-3-5-sonnet}'),
    ('gemini', 'Google Gemini', '{CHAT,EMBEDDING,VISION}', '{gemini-1.5-flash,gemini-2.5-flash,gemini-1.5-pro}'),
    ('deepseek', 'DeepSeek', '{CHAT}', '{deepseek-chat,deepseek-reasoner}'),
    ('groq', 'Groq', '{CHAT}', '{llama-3.1-8b-instant,llama-3.1-70b-versatile}'),
    ('mistral', 'Mistral AI', '{CHAT}', '{mistral-small-latest,mistral-large-latest}'),
    ('cohere', 'Cohere', '{CHAT,EMBEDDING,RERANK}', '{command-r,command-r-plus}'),
    ('perplexity', 'Perplexity', '{CHAT,SEARCH}', '{sonar,sonar-pro}');

-- Seed default models
INSERT INTO ai_gateway.models (provider_id, model_id, display_name, category, context_window, max_output_tokens, input_cost_per_1k, output_cost_per_1k, is_default, tier)
SELECT p.id, m.model_id, m.display_name, 'chat', m.context_window, m.max_output_tokens, m.input_cost, m.output_cost, m.is_default, m.tier
FROM ai_gateway.providers p
CROSS JOIN LATERAL (VALUES
    ('openai', 'gpt-4o-mini', 'GPT-4o Mini', 128000, 16384, 0.00015, 0.0006, TRUE, 'fast'),
    ('openai', 'gpt-4.1-mini', 'GPT-4.1 Mini', 1000000, 32768, 0.0004, 0.0016, FALSE, 'balanced'),
    ('anthropic', 'claude-3-5-haiku', 'Claude 3.5 Haiku', 200000, 8192, 0.0008, 0.004, TRUE, 'fast'),
    ('gemini', 'gemini-1.5-flash', 'Gemini 1.5 Flash', 1000000, 8192, 0.000075, 0.0003, TRUE, 'fast'),
    ('gemini', 'gemini-2.5-flash', 'Gemini 2.5 Flash', 1000000, 65536, 0.00015, 0.0006, FALSE, 'balanced'),
    ('deepseek', 'deepseek-chat', 'DeepSeek Chat', 64000, 8192, 0.00014, 0.00028, TRUE, 'fast'),
    ('deepseek', 'deepseek-reasoner', 'DeepSeek Reasoner', 64000, 8192, 0.00055, 0.0022, FALSE, 'powerful'),
    ('groq', 'llama-3.1-8b-instant', 'Llama 3.1 8B', 131072, 8192, 0.00005, 0.00008, TRUE, 'fast'),
    ('groq', 'llama-3.1-70b-versatile', 'Llama 3.1 70B', 131072, 32768, 0.00059, 0.00079, FALSE, 'balanced'),
    ('mistral', 'mistral-small-latest', 'Mistral Small', 32000, 8192, 0.001, 0.003, TRUE, 'fast'),
    ('mistral', 'mistral-large-latest', 'Mistral Large', 128000, 8192, 0.004, 0.012, FALSE, 'powerful'),
    ('cohere', 'command-r', 'Command R', 128000, 4096, 0.00015, 0.0006, TRUE, 'balanced'),
    ('cohere', 'command-r-plus', 'Command R+', 128000, 4096, 0.003, 0.015, FALSE, 'powerful'),
    ('perplexity', 'sonar', 'Sonar', 127000, 8192, 0.001, 0.001, TRUE, 'balanced'),
    ('perplexity', 'sonar-pro', 'Sonar Pro', 127000, 8192, 0.003, 0.015, FALSE, 'powerful')
) AS m(provider_name, model_id, display_name, context_window, max_output_tokens, input_cost, output_cost, is_default, tier)
WHERE p.name = m.provider_name;
