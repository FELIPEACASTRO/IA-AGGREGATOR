-- =============================================================================
-- V11: Execution, Platform, and Marketplace tables + FK fix
-- =============================================================================

-- Fix missing FK on knowledge_chunks.document_id
ALTER TABLE content.knowledge_chunks
    ADD CONSTRAINT fk_chunk_document
    FOREIGN KEY (document_id) REFERENCES content.knowledge_documents(id);

-- =============================================================================
-- Execution schema
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS execution;

-- Tools
CREATE TABLE execution.tools (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    schema_json     TEXT,
    scope           VARCHAR(50) DEFAULT 'INTERNAL',
    risk_level      VARCHAR(50) DEFAULT 'LOW',
    estimated_cost_usd NUMERIC(10,6) DEFAULT 0,
    endpoint        TEXT,
    headers         JSONB DEFAULT '{}',
    requires_approval BOOLEAN DEFAULT FALSE,
    enabled         BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Workflows
CREATE TABLE execution.workflows (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    steps           JSONB DEFAULT '[]',
    trigger_config  JSONB DEFAULT '{}',
    status          VARCHAR(50) DEFAULT 'DRAFT',
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Workflow runs
CREATE TABLE execution.workflow_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id     UUID NOT NULL REFERENCES execution.workflows(id),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    triggered_by    UUID REFERENCES auth.users(id),
    status          VARCHAR(50) DEFAULT 'PENDING',
    current_step_id VARCHAR(100),
    step_history    JSONB DEFAULT '[]',
    context         JSONB DEFAULT '{}',
    error_message   TEXT,
    started_at      TIMESTAMPTZ DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

-- Agent executions
CREATE TABLE execution.agent_executions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_definition_id UUID NOT NULL,
    org_id              UUID NOT NULL REFERENCES auth.organizations(id),
    user_id             UUID NOT NULL REFERENCES auth.users(id),
    objective           TEXT,
    autonomy_level      VARCHAR(50) DEFAULT 'L0_ASSISTIVE',
    status              VARCHAR(50) DEFAULT 'PLANNING',
    steps               JSONB DEFAULT '[]',
    total_cost_usd      NUMERIC(12,6) DEFAULT 0,
    max_budget_usd      NUMERIC(12,6) DEFAULT 0,
    final_answer        TEXT,
    error_message       TEXT,
    started_at          TIMESTAMPTZ DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

-- =============================================================================
-- Platform schema
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS platform;

-- Virtual keys
CREATE TABLE platform.virtual_keys (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES auth.organizations(id),
    created_by          UUID NOT NULL REFERENCES auth.users(id),
    name                VARCHAR(200) NOT NULL,
    key_hash            VARCHAR(128) NOT NULL UNIQUE,
    key_prefix          VARCHAR(20) NOT NULL,
    allowed_models      TEXT[] DEFAULT '{}',
    allowed_capabilities TEXT[] DEFAULT '{}',
    rate_limit_rpm      INTEGER DEFAULT 60,
    budget_limit_usd    NUMERIC(12,4) DEFAULT 0,
    spent_usd           NUMERIC(12,4) DEFAULT 0,
    enabled             BOOLEAN DEFAULT TRUE,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    last_used_at        TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ
);

-- Request traces
CREATE TABLE platform.request_traces (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES auth.organizations(id),
    virtual_key_id      UUID REFERENCES platform.virtual_keys(id),
    capability          VARCHAR(100),
    requested_model     VARCHAR(200),
    used_model          VARCHAR(200),
    provider            VARCHAR(100),
    status_code         INTEGER DEFAULT 200,
    input_tokens        BIGINT DEFAULT 0,
    output_tokens       BIGINT DEFAULT 0,
    cost_usd            NUMERIC(12,6) DEFAULT 0,
    total_latency_ms    BIGINT DEFAULT 0,
    provider_latency_ms BIGINT DEFAULT 0,
    routing_latency_ms  BIGINT DEFAULT 0,
    cache_hit           BOOLEAN DEFAULT FALSE,
    fallback_used       BOOLEAN DEFAULT FALSE,
    error_code          VARCHAR(50),
    error_message       TEXT,
    timestamp           TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_request_traces_org_ts ON platform.request_traces(org_id, timestamp);
CREATE INDEX idx_request_traces_vkey_ts ON platform.request_traces(virtual_key_id, timestamp);

-- Batch jobs
CREATE TABLE platform.batch_jobs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id              UUID NOT NULL REFERENCES auth.organizations(id),
    webhook_url         TEXT,
    total_requests      INTEGER DEFAULT 0,
    completed_requests  INTEGER DEFAULT 0,
    failed_requests     INTEGER DEFAULT 0,
    status              VARCHAR(50) DEFAULT 'QUEUED',
    total_cost_usd      NUMERIC(12,6) DEFAULT 0,
    discount_percent    NUMERIC(5,2) DEFAULT 0,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

-- =============================================================================
-- Marketplace schema
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS marketplace;

-- Catalog entries
CREATE TABLE marketplace.catalog_entries (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publisher_id          UUID NOT NULL REFERENCES auth.organizations(id),
    name                  VARCHAR(200) NOT NULL,
    description           TEXT,
    entry_type            VARCHAR(50) NOT NULL,
    categories            TEXT[] DEFAULT '{}',
    tags                  TEXT[] DEFAULT '{}',
    version               VARCHAR(50),
    price                 NUMERIC(10,4) DEFAULT 0,
    revenue_share_percent NUMERIC(5,2) DEFAULT 0,
    average_rating        NUMERIC(3,2) DEFAULT 0,
    install_count         BIGINT DEFAULT 0,
    status                VARCHAR(50) DEFAULT 'DRAFT',
    metadata              JSONB DEFAULT '{}',
    published_at          TIMESTAMPTZ,
    updated_at            TIMESTAMPTZ DEFAULT NOW()
);

-- Catalog reviews
CREATE TABLE marketplace.catalog_reviews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id    UUID NOT NULL REFERENCES marketplace.catalog_entries(id),
    user_id     UUID NOT NULL REFERENCES auth.users(id),
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(entry_id, user_id)
);
