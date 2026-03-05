-- ============================================================================
-- V4: Analytics ingestion persistence
-- ============================================================================

SET search_path TO analytics, public;

CREATE TABLE IF NOT EXISTS analytics.event_reports (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source          VARCHAR(100) NOT NULL,
    generated_at    TIMESTAMPTZ,
    total_events    INTEGER NOT NULL DEFAULT 0,
    counters        JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_payload     JSONB NOT NULL,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_event_reports_source ON analytics.event_reports(source);
CREATE INDEX IF NOT EXISTS idx_event_reports_received_at ON analytics.event_reports(received_at DESC);

CREATE TABLE IF NOT EXISTS analytics.ingested_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id       UUID NOT NULL REFERENCES analytics.event_reports(id) ON DELETE CASCADE,
    event_name      VARCHAR(120) NOT NULL,
    event_category  event_category NOT NULL DEFAULT 'system',
    event_timestamp TIMESTAMPTZ,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ingested_events_report_id ON analytics.ingested_events(report_id);
CREATE INDEX IF NOT EXISTS idx_ingested_events_event_name ON analytics.ingested_events(event_name);
CREATE INDEX IF NOT EXISTS idx_ingested_events_event_category ON analytics.ingested_events(event_category);
CREATE INDEX IF NOT EXISTS idx_ingested_events_event_timestamp ON analytics.ingested_events(event_timestamp DESC);
