-- ============================================================================
-- IA AGGREGATOR - Module 8: ANALYTICS
-- File: 08_analytics.sql
-- Purpose: Usage metrics, events, aggregated reports, dashboards
-- ============================================================================

SET search_path TO analytics, public;

-- ============================================================================
-- EVENTS (raw event stream)
-- ============================================================================

CREATE TABLE analytics.events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Who
    user_id         UUID REFERENCES auth.users(id),
    org_id          UUID REFERENCES auth.organizations(id),
    session_id      UUID, -- browser/API session
    -- What
    event_name      VARCHAR(100) NOT NULL, -- e.g., 'conversation.started', 'message.sent', 'model.switched'
    event_category  event_category NOT NULL,
    -- Context
    properties      JSONB NOT NULL DEFAULT '{}'::jsonb, -- event-specific data
    -- Source
    source          VARCHAR(50) NOT NULL DEFAULT 'web', -- 'web', 'api', 'mobile', 'system'
    ip_address      INET,
    user_agent      TEXT,
    -- Geo (from IP)
    country         VARCHAR(2),
    region          VARCHAR(100),
    city            VARCHAR(100),
    -- Page/Screen context
    page_url        TEXT,
    referrer_url    TEXT,
    -- Device
    device_type     VARCHAR(20),
    browser         VARCHAR(50),
    os              VARCHAR(50),
    -- Timing
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE analytics.events IS 'Raw event stream for all platform activity. High-volume, append-only.';
COMMENT ON COLUMN analytics.events.event_name IS 'Dot-notation event name: auth.login, chat.message_sent, billing.subscription_created, etc.';
COMMENT ON COLUMN analytics.events.properties IS 'Event-specific JSON payload: {model_id, tokens_used, plan_tier, etc.}';

-- Partition by month for performance (noted in migration strategy)
CREATE INDEX idx_events_user ON analytics.events(user_id);
CREATE INDEX idx_events_org ON analytics.events(org_id);
CREATE INDEX idx_events_name ON analytics.events(event_name);
CREATE INDEX idx_events_category ON analytics.events(event_category);
CREATE INDEX idx_events_created ON analytics.events(created_at);
CREATE INDEX idx_events_user_created ON analytics.events(user_id, created_at DESC);
CREATE INDEX idx_events_org_created ON analytics.events(org_id, created_at DESC);

-- ============================================================================
-- USAGE METRICS (pre-aggregated daily/hourly summaries)
-- ============================================================================

CREATE TABLE analytics.usage_metrics_daily (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Dimensions
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    user_id         UUID REFERENCES auth.users(id), -- NULL for org-level aggregation
    model_id        UUID REFERENCES ai_gateway.models(id), -- NULL for total across models
    -- Time dimension
    metric_date     DATE NOT NULL,
    -- Request metrics
    total_requests  BIGINT NOT NULL DEFAULT 0,
    successful_requests BIGINT NOT NULL DEFAULT 0,
    failed_requests BIGINT NOT NULL DEFAULT 0,
    cached_requests BIGINT NOT NULL DEFAULT 0,
    -- Token metrics
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens    BIGINT NOT NULL DEFAULT 0,
    -- Credit metrics
    total_credits_used BIGINT NOT NULL DEFAULT 0,
    -- Cost metrics (in BRL centavos, platform cost)
    total_cost_cents BIGINT NOT NULL DEFAULT 0,
    -- Performance metrics
    avg_latency_ms  INTEGER,
    p50_latency_ms  INTEGER,
    p95_latency_ms  INTEGER,
    p99_latency_ms  INTEGER,
    avg_ttft_ms     INTEGER, -- time to first token
    -- Conversation metrics
    conversations_created INTEGER NOT NULL DEFAULT 0,
    messages_sent   INTEGER NOT NULL DEFAULT 0,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_usage_daily UNIQUE (org_id, user_id, model_id, metric_date)
);

COMMENT ON TABLE analytics.usage_metrics_daily IS 'Pre-aggregated daily usage metrics. Materialized from raw events/requests for fast dashboard queries.';

CREATE INDEX idx_usage_daily_org ON analytics.usage_metrics_daily(org_id);
CREATE INDEX idx_usage_daily_user ON analytics.usage_metrics_daily(user_id);
CREATE INDEX idx_usage_daily_model ON analytics.usage_metrics_daily(model_id);
CREATE INDEX idx_usage_daily_date ON analytics.usage_metrics_daily(metric_date);
CREATE INDEX idx_usage_daily_org_date ON analytics.usage_metrics_daily(org_id, metric_date DESC);

CREATE TRIGGER trg_usage_daily_updated_at
    BEFORE UPDATE ON analytics.usage_metrics_daily
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- USAGE METRICS HOURLY (for real-time dashboards)
-- ============================================================================

CREATE TABLE analytics.usage_metrics_hourly (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    user_id         UUID REFERENCES auth.users(id),
    model_id        UUID REFERENCES ai_gateway.models(id),
    -- Time dimension
    metric_hour     TIMESTAMPTZ NOT NULL, -- truncated to hour
    -- Metrics (same structure, smaller granularity)
    total_requests  BIGINT NOT NULL DEFAULT 0,
    successful_requests BIGINT NOT NULL DEFAULT 0,
    failed_requests BIGINT NOT NULL DEFAULT 0,
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    total_credits_used BIGINT NOT NULL DEFAULT 0,
    avg_latency_ms  INTEGER,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_usage_hourly UNIQUE (org_id, user_id, model_id, metric_hour)
);

COMMENT ON TABLE analytics.usage_metrics_hourly IS 'Hourly usage metrics for real-time monitoring. Retained for 90 days, then rolled into daily.';

CREATE INDEX idx_usage_hourly_org_hour ON analytics.usage_metrics_hourly(org_id, metric_hour DESC);
CREATE INDEX idx_usage_hourly_hour ON analytics.usage_metrics_hourly(metric_hour);

-- ============================================================================
-- MODEL LEADERBOARD (aggregated model performance)
-- ============================================================================

CREATE TABLE analytics.model_leaderboard (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_id        UUID NOT NULL REFERENCES ai_gateway.models(id),
    -- Time period
    period          aggregation_period NOT NULL,
    period_start    TIMESTAMPTZ NOT NULL,
    -- Usage stats
    total_requests  BIGINT NOT NULL DEFAULT 0,
    unique_users    INTEGER NOT NULL DEFAULT 0,
    -- Quality
    avg_user_rating NUMERIC(3,2),
    total_ratings   INTEGER NOT NULL DEFAULT 0,
    positive_rating_pct NUMERIC(5,2),
    -- Performance
    avg_latency_ms  INTEGER,
    avg_ttft_ms     INTEGER,
    error_rate      NUMERIC(5,2),
    -- Cost efficiency
    avg_credits_per_request NUMERIC(10,4),
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_model_leaderboard UNIQUE (model_id, period, period_start)
);

COMMENT ON TABLE analytics.model_leaderboard IS 'Aggregated model performance metrics for comparison and recommendation.';

CREATE INDEX idx_leaderboard_model ON analytics.model_leaderboard(model_id);
CREATE INDEX idx_leaderboard_period ON analytics.model_leaderboard(period, period_start DESC);

-- ============================================================================
-- PLATFORM METRICS (system-wide health)
-- ============================================================================

CREATE TABLE analytics.platform_metrics (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_time     TIMESTAMPTZ NOT NULL,
    -- User metrics
    total_users     BIGINT NOT NULL DEFAULT 0,
    active_users_daily BIGINT NOT NULL DEFAULT 0,
    active_users_weekly BIGINT NOT NULL DEFAULT 0,
    active_users_monthly BIGINT NOT NULL DEFAULT 0,
    new_signups     INTEGER NOT NULL DEFAULT 0,
    -- Subscription metrics
    total_subscribers BIGINT NOT NULL DEFAULT 0,
    mrr_cents       BIGINT NOT NULL DEFAULT 0, -- Monthly Recurring Revenue
    arr_cents       BIGINT NOT NULL DEFAULT 0, -- Annual Recurring Revenue
    churn_rate      NUMERIC(5,2),
    -- Plan breakdown
    free_users      BIGINT NOT NULL DEFAULT 0,
    starter_users   BIGINT NOT NULL DEFAULT 0,
    pro_users       BIGINT NOT NULL DEFAULT 0,
    team_users      BIGINT NOT NULL DEFAULT 0,
    enterprise_users BIGINT NOT NULL DEFAULT 0,
    -- Usage metrics
    total_api_requests BIGINT NOT NULL DEFAULT 0,
    total_tokens_processed BIGINT NOT NULL DEFAULT 0,
    total_credits_consumed BIGINT NOT NULL DEFAULT 0,
    -- Partner metrics
    total_partners  INTEGER NOT NULL DEFAULT 0,
    partner_revenue_cents BIGINT NOT NULL DEFAULT 0,
    -- Performance
    avg_response_time_ms INTEGER,
    uptime_percentage NUMERIC(5,2),
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE analytics.platform_metrics IS 'Platform-wide metrics snapshot. Captured periodically (hourly/daily) for admin dashboards.';

CREATE INDEX idx_platform_metrics_time ON analytics.platform_metrics(metric_time DESC);

-- ============================================================================
-- REPORTS (saved/scheduled reports)
-- ============================================================================

CREATE TABLE analytics.reports (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Ownership
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID REFERENCES auth.organizations(id),
    -- Report definition
    name            VARCHAR(200) NOT NULL,
    report_type     report_type NOT NULL,
    -- Configuration
    config          JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- e.g., {date_range, models, group_by, metrics, filters}
    -- Schedule (for recurring reports)
    is_scheduled    BOOLEAN NOT NULL DEFAULT FALSE,
    schedule_cron   VARCHAR(100), -- cron expression
    schedule_timezone VARCHAR(50) DEFAULT 'America/Sao_Paulo',
    last_run_at     TIMESTAMPTZ,
    next_run_at     TIMESTAMPTZ,
    -- Delivery
    delivery_email  VARCHAR(320),
    delivery_webhook TEXT,
    -- Last generated report
    last_result     JSONB,
    last_result_url TEXT, -- S3 URL for exported CSV/PDF
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE analytics.reports IS 'Saved and scheduled analytics reports. Supports email/webhook delivery.';
COMMENT ON COLUMN analytics.reports.config IS 'JSON report configuration: {date_range, metrics, group_by, filters, format}.';

CREATE INDEX idx_reports_creator ON analytics.reports(created_by);
CREATE INDEX idx_reports_org ON analytics.reports(org_id);
CREATE INDEX idx_reports_scheduled ON analytics.reports(is_scheduled, next_run_at)
    WHERE is_scheduled = TRUE AND is_active = TRUE;

CREATE TRIGGER trg_reports_updated_at
    BEFORE UPDATE ON analytics.reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- REPORT EXECUTIONS (history of report runs)
-- ============================================================================

CREATE TABLE analytics.report_executions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id       UUID NOT NULL REFERENCES analytics.reports(id) ON DELETE CASCADE,
    -- Execution details
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    -- Result
    status          VARCHAR(20) NOT NULL DEFAULT 'running', -- running, completed, failed
    result_summary  JSONB,
    result_url      TEXT,
    row_count       INTEGER,
    -- Error
    error_message   TEXT,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE analytics.report_executions IS 'History of report generation runs. Tracks timing, status, and output.';

CREATE INDEX idx_report_executions_report ON analytics.report_executions(report_id);
CREATE INDEX idx_report_executions_status ON analytics.report_executions(status);

-- ============================================================================
-- USER ACTIVITY SUMMARY (per-user engagement metrics)
-- ============================================================================

CREATE TABLE analytics.user_activity_summary (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    -- Time period
    summary_date    DATE NOT NULL,
    -- Activity
    conversations_created INTEGER NOT NULL DEFAULT 0,
    messages_sent   INTEGER NOT NULL DEFAULT 0,
    models_used     TEXT[] DEFAULT ARRAY[]::TEXT[],
    active_minutes  INTEGER NOT NULL DEFAULT 0, -- estimated active time
    -- Feature usage
    used_personas   BOOLEAN NOT NULL DEFAULT FALSE,
    used_templates  BOOLEAN NOT NULL DEFAULT FALSE,
    used_rag        BOOLEAN NOT NULL DEFAULT FALSE,
    used_api        BOOLEAN NOT NULL DEFAULT FALSE,
    used_sharing    BOOLEAN NOT NULL DEFAULT FALSE,
    -- Credits
    credits_used    BIGINT NOT NULL DEFAULT 0,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_activity_daily UNIQUE (user_id, summary_date)
);

COMMENT ON TABLE analytics.user_activity_summary IS 'Daily per-user engagement summary. Used for retention analysis and feature adoption tracking.';

CREATE INDEX idx_user_activity_user ON analytics.user_activity_summary(user_id);
CREATE INDEX idx_user_activity_date ON analytics.user_activity_summary(summary_date);
