-- ============================================================================
-- V10: Audit Module Tables
-- ============================================================================

SET search_path TO audit, auth, public;

-- Audit Logs
CREATE TABLE audit.logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID REFERENCES auth.organizations(id),
    user_id         UUID REFERENCES auth.users(id),
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(100),
    target_id       UUID,
    ip_address      INET,
    user_agent      TEXT,
    details         JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_org ON audit.logs(org_id);
CREATE INDEX idx_audit_logs_user ON audit.logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit.logs(action);
CREATE INDEX idx_audit_logs_target ON audit.logs(target_type, target_id);
CREATE INDEX idx_audit_logs_created ON audit.logs(created_at);

-- Partition by month for performance
-- CREATE TABLE audit.logs_2026_03 PARTITION OF audit.logs
--     FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
