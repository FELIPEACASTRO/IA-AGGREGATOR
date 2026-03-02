-- ============================================================================
-- IA AGGREGATOR - Cross-Cutting Concerns
-- File: 09_audit_and_rls.sql
-- Purpose: Audit log, Row-Level Security policies, migration notes
-- ============================================================================

-- ============================================================================
-- AUDIT LOG
-- ============================================================================

CREATE TABLE audit.audit_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Who
    user_id         UUID, -- NULL for system actions
    org_id          UUID,
    ip_address      INET,
    user_agent      TEXT,
    -- What
    action          VARCHAR(100) NOT NULL, -- 'create', 'update', 'delete', 'login', 'export', etc.
    resource_type   VARCHAR(100) NOT NULL, -- 'user', 'conversation', 'subscription', etc.
    resource_id     UUID,
    -- Details
    old_values      JSONB, -- previous state (for updates)
    new_values      JSONB, -- new state (for creates/updates)
    changed_fields  TEXT[], -- which fields changed
    -- Context
    request_id      UUID, -- correlation ID for the API request
    session_id      UUID,
    api_key_id      UUID,
    -- Result
    status          VARCHAR(20) NOT NULL DEFAULT 'success', -- 'success', 'failure', 'denied'
    error_message   TEXT,
    -- LGPD: flag if this involves personal data
    involves_pii    BOOLEAN NOT NULL DEFAULT FALSE,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE audit.audit_log IS 'Immutable audit trail of all significant actions. LGPD compliance requirement. Never updated or deleted (except by data retention policy).';
COMMENT ON COLUMN audit.audit_log.old_values IS 'Snapshot of previous values for update operations. PII fields are masked.';
COMMENT ON COLUMN audit.audit_log.involves_pii IS 'Flag for LGPD: TRUE if this action involves personally identifiable information.';

-- High-performance indexes for the audit log
CREATE INDEX idx_audit_user ON audit.audit_log(user_id);
CREATE INDEX idx_audit_org ON audit.audit_log(org_id);
CREATE INDEX idx_audit_action ON audit.audit_log(action);
CREATE INDEX idx_audit_resource ON audit.audit_log(resource_type, resource_id);
CREATE INDEX idx_audit_created ON audit.audit_log(created_at);
CREATE INDEX idx_audit_user_created ON audit.audit_log(user_id, created_at DESC);
CREATE INDEX idx_audit_org_created ON audit.audit_log(org_id, created_at DESC);
CREATE INDEX idx_audit_request ON audit.audit_log(request_id) WHERE request_id IS NOT NULL;
CREATE INDEX idx_audit_pii ON audit.audit_log(involves_pii) WHERE involves_pii = TRUE;

-- ============================================================================
-- AUDIT LOG: GENERIC TRIGGER FUNCTION
-- ============================================================================

CREATE OR REPLACE FUNCTION audit.log_changes()
RETURNS TRIGGER AS $$
DECLARE
    old_data JSONB;
    new_data JSONB;
    changed TEXT[];
    col TEXT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        old_data := to_jsonb(OLD);
        INSERT INTO audit.audit_log (
            user_id, action, resource_type, resource_id,
            old_values, status
        ) VALUES (
            get_current_user_id(), 'delete', TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
            (OLD).id, old_data, 'success'
        );
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        old_data := to_jsonb(OLD);
        new_data := to_jsonb(NEW);
        -- Find changed fields
        changed := ARRAY(
            SELECT key FROM jsonb_each(new_data)
            WHERE new_data->key IS DISTINCT FROM old_data->key
            AND key NOT IN ('updated_at')
        );
        -- Only log if something actually changed
        IF array_length(changed, 1) > 0 THEN
            INSERT INTO audit.audit_log (
                user_id, action, resource_type, resource_id,
                old_values, new_values, changed_fields, status
            ) VALUES (
                get_current_user_id(), 'update', TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
                (NEW).id, old_data, new_data, changed, 'success'
            );
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        new_data := to_jsonb(NEW);
        INSERT INTO audit.audit_log (
            user_id, action, resource_type, resource_id,
            new_values, status
        ) VALUES (
            get_current_user_id(), 'create', TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME,
            (NEW).id, new_data, 'success'
        );
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION audit.log_changes() IS 'Generic audit trigger function. Attach to tables requiring audit trail.';

-- ============================================================================
-- ATTACH AUDIT TRIGGERS TO CRITICAL TABLES
-- ============================================================================

-- Auth module
CREATE TRIGGER audit_users
    AFTER INSERT OR UPDATE OR DELETE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

CREATE TRIGGER audit_organizations
    AFTER INSERT OR UPDATE OR DELETE ON auth.organizations
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

CREATE TRIGGER audit_api_keys
    AFTER INSERT OR UPDATE OR DELETE ON auth.api_keys
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

-- Billing module
CREATE TRIGGER audit_subscriptions
    AFTER INSERT OR UPDATE OR DELETE ON billing.subscriptions
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

CREATE TRIGGER audit_payments
    AFTER INSERT OR UPDATE OR DELETE ON billing.payments
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

-- Partners module
CREATE TRIGGER audit_partners
    AFTER INSERT OR UPDATE OR DELETE ON partners.partners
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

CREATE TRIGGER audit_commissions
    AFTER INSERT OR UPDATE OR DELETE ON partners.commissions
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

CREATE TRIGGER audit_payment_batches
    AFTER INSERT OR UPDATE OR DELETE ON partners.payment_batches
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

-- Teams module
CREATE TRIGGER audit_workspace_members
    AFTER INSERT OR UPDATE OR DELETE ON teams.workspace_members
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

CREATE TRIGGER audit_org_members
    AFTER INSERT OR UPDATE OR DELETE ON teams.org_members
    FOR EACH ROW EXECUTE FUNCTION audit.log_changes();

-- ============================================================================
-- ROW-LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

-- Enable RLS on all multi-tenant tables

-- === AUTH ===

ALTER TABLE auth.users ENABLE ROW LEVEL SECURITY;

CREATE POLICY users_self_access ON auth.users
    FOR ALL
    USING (id = get_current_user_id() OR is_super_admin());

CREATE POLICY users_org_read ON auth.users
    FOR SELECT
    USING (
        personal_org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

ALTER TABLE auth.api_keys ENABLE ROW LEVEL SECURITY;

CREATE POLICY api_keys_owner ON auth.api_keys
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

ALTER TABLE auth.sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY sessions_owner ON auth.sessions
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

ALTER TABLE auth.consent_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY consent_owner ON auth.consent_records
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

ALTER TABLE auth.notification_preferences ENABLE ROW LEVEL SECURITY;

CREATE POLICY notification_prefs_owner ON auth.notification_preferences
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

-- === BILLING ===

ALTER TABLE billing.subscriptions ENABLE ROW LEVEL SECURITY;

CREATE POLICY subscriptions_org_access ON billing.subscriptions
    FOR ALL
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

ALTER TABLE billing.credit_balances ENABLE ROW LEVEL SECURITY;

CREATE POLICY credit_balances_org_access ON billing.credit_balances
    FOR SELECT
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

ALTER TABLE billing.credit_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY credit_tx_org_access ON billing.credit_transactions
    FOR SELECT
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

ALTER TABLE billing.payments ENABLE ROW LEVEL SECURITY;

CREATE POLICY payments_org_access ON billing.payments
    FOR ALL
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

ALTER TABLE billing.invoices ENABLE ROW LEVEL SECURITY;

CREATE POLICY invoices_org_access ON billing.invoices
    FOR SELECT
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

ALTER TABLE billing.payment_methods ENABLE ROW LEVEL SECURITY;

CREATE POLICY payment_methods_owner ON billing.payment_methods
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

-- === CHAT ===

ALTER TABLE chat.conversations ENABLE ROW LEVEL SECURITY;

CREATE POLICY conversations_owner ON chat.conversations
    FOR ALL
    USING (
        user_id = get_current_user_id()
        OR id IN (
            SELECT conversation_id FROM chat.conversation_participants
            WHERE user_id = get_current_user_id()
        )
        OR is_super_admin()
    );

ALTER TABLE chat.messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY messages_conversation_access ON chat.messages
    FOR ALL
    USING (
        conversation_id IN (
            SELECT id FROM chat.conversations
            WHERE user_id = get_current_user_id()
        )
        OR conversation_id IN (
            SELECT conversation_id FROM chat.conversation_participants
            WHERE user_id = get_current_user_id()
        )
        OR is_super_admin()
    );

ALTER TABLE chat.folders ENABLE ROW LEVEL SECURITY;

CREATE POLICY folders_owner ON chat.folders
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

ALTER TABLE chat.shared_links ENABLE ROW LEVEL SECURITY;

CREATE POLICY shared_links_owner ON chat.shared_links
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

-- Public access for viewing shared links (read-only, checked in application layer)
CREATE POLICY shared_links_public_read ON chat.shared_links
    FOR SELECT
    USING (is_active = TRUE AND (expires_at IS NULL OR expires_at > NOW()));

ALTER TABLE chat.saved_prompts ENABLE ROW LEVEL SECURITY;

CREATE POLICY saved_prompts_owner ON chat.saved_prompts
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

-- === CONTENT ===

ALTER TABLE content.personas ENABLE ROW LEVEL SECURITY;

CREATE POLICY personas_access ON content.personas
    FOR SELECT
    USING (
        created_by = get_current_user_id()
        OR visibility = 'public'
        OR (visibility = 'workspace' AND org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        ))
        OR is_super_admin()
    );

CREATE POLICY personas_modify ON content.personas
    FOR ALL
    USING (created_by = get_current_user_id() OR is_super_admin());

ALTER TABLE content.prompt_templates ENABLE ROW LEVEL SECURITY;

CREATE POLICY templates_access ON content.prompt_templates
    FOR SELECT
    USING (
        created_by = get_current_user_id()
        OR visibility = 'public'
        OR (visibility = 'workspace' AND org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        ))
        OR is_super_admin()
    );

CREATE POLICY templates_modify ON content.prompt_templates
    FOR ALL
    USING (created_by = get_current_user_id() OR is_super_admin());

ALTER TABLE content.knowledge_bases ENABLE ROW LEVEL SECURITY;

CREATE POLICY kb_org_access ON content.knowledge_bases
    FOR ALL
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

-- === PARTNERS ===

ALTER TABLE partners.partners ENABLE ROW LEVEL SECURITY;

CREATE POLICY partners_self_access ON partners.partners
    FOR ALL
    USING (user_id = get_current_user_id() OR is_super_admin());

ALTER TABLE partners.commissions ENABLE ROW LEVEL SECURITY;

CREATE POLICY commissions_partner_access ON partners.commissions
    FOR SELECT
    USING (
        partner_id IN (
            SELECT id FROM partners.partners WHERE user_id = get_current_user_id()
        )
        OR is_super_admin()
    );

ALTER TABLE partners.referral_links ENABLE ROW LEVEL SECURITY;

CREATE POLICY referral_links_partner ON partners.referral_links
    FOR ALL
    USING (
        partner_id IN (
            SELECT id FROM partners.partners WHERE user_id = get_current_user_id()
        )
        OR is_super_admin()
    );

ALTER TABLE partners.clicks ENABLE ROW LEVEL SECURITY;

CREATE POLICY clicks_partner_access ON partners.clicks
    FOR SELECT
    USING (
        partner_id IN (
            SELECT id FROM partners.partners WHERE user_id = get_current_user_id()
        )
        OR is_super_admin()
    );

ALTER TABLE partners.attributions ENABLE ROW LEVEL SECURITY;

CREATE POLICY attributions_partner_access ON partners.attributions
    FOR SELECT
    USING (
        partner_id IN (
            SELECT id FROM partners.partners WHERE user_id = get_current_user_id()
        )
        OR is_super_admin()
    );

-- === TEAMS ===

ALTER TABLE teams.workspaces ENABLE ROW LEVEL SECURITY;

CREATE POLICY workspaces_org_access ON teams.workspaces
    FOR SELECT
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

CREATE POLICY workspaces_admin_modify ON teams.workspaces
    FOR ALL
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id()
              AND role IN ('owner', 'admin')
              AND is_active = TRUE
        )
        OR is_super_admin()
    );

ALTER TABLE teams.workspace_members ENABLE ROW LEVEL SECURITY;

CREATE POLICY ws_members_access ON teams.workspace_members
    FOR SELECT
    USING (
        workspace_id IN (
            SELECT id FROM teams.workspaces WHERE org_id IN (
                SELECT org_id FROM teams.org_members
                WHERE user_id = get_current_user_id() AND is_active = TRUE
            )
        )
        OR is_super_admin()
    );

ALTER TABLE teams.org_members ENABLE ROW LEVEL SECURITY;

CREATE POLICY org_members_access ON teams.org_members
    FOR SELECT
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members om
            WHERE om.user_id = get_current_user_id() AND om.is_active = TRUE
        )
        OR is_super_admin()
    );

-- === ANALYTICS ===

ALTER TABLE analytics.events ENABLE ROW LEVEL SECURITY;

CREATE POLICY events_org_access ON analytics.events
    FOR SELECT
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR user_id = get_current_user_id()
        OR is_super_admin()
    );

ALTER TABLE analytics.usage_metrics_daily ENABLE ROW LEVEL SECURITY;

CREATE POLICY usage_daily_org_access ON analytics.usage_metrics_daily
    FOR SELECT
    USING (
        org_id IN (
            SELECT org_id FROM teams.org_members
            WHERE user_id = get_current_user_id() AND is_active = TRUE
        )
        OR is_super_admin()
    );

ALTER TABLE analytics.reports ENABLE ROW LEVEL SECURITY;

CREATE POLICY reports_owner ON analytics.reports
    FOR ALL
    USING (created_by = get_current_user_id() OR is_super_admin());

-- ============================================================================
-- SERVICE ROLE BYPASS (for backend services)
-- ============================================================================

-- Create a service role that bypasses RLS for backend operations
-- Application code uses this role for internal operations

-- DO $$
-- BEGIN
--     IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'service_role') THEN
--         CREATE ROLE service_role NOLOGIN;
--     END IF;
-- END
-- $$;

-- Grant service_role access to all schemas
-- GRANT USAGE ON SCHEMA auth, billing, chat, ai_gateway, partners, content, teams, analytics, audit TO service_role;
-- GRANT ALL ON ALL TABLES IN SCHEMA auth, billing, chat, ai_gateway, partners, content, teams, analytics, audit TO service_role;

-- Service role bypasses RLS
-- ALTER DEFAULT PRIVILEGES IN SCHEMA auth, billing, chat, ai_gateway, partners, content, teams, analytics, audit
--     GRANT ALL ON TABLES TO service_role;

-- ============================================================================
-- MIGRATION STRATEGY NOTES
-- ============================================================================

/*
=== MIGRATION STRATEGY ===

1. EXECUTION ORDER:
   Run migration files in numerical order:
     00_extensions_and_types.sql  -- Extensions, enums, functions, schemas
     01_auth.sql                  -- Auth module (no dependencies)
     02_billing.sql               -- Billing (depends on auth)
     03_chat.sql                  -- Chat (depends on auth)
     04_ai_gateway.sql            -- AI Gateway (depends on auth, adds FKs to chat/billing)
     05_partners.sql              -- Partners (depends on auth, billing)
     06_content.sql               -- Content (depends on auth, ai_gateway, chat)
     07_teams.sql                 -- Teams (depends on auth, ai_gateway, content)
     08_analytics.sql             -- Analytics (depends on auth, ai_gateway)
     09_audit_and_rls.sql         -- Audit + RLS (depends on ALL other modules)

2. TABLE PARTITIONING (recommended for production):
   These high-volume tables should be partitioned by time:

   - analytics.events             -> PARTITION BY RANGE (created_at), monthly
   - ai_gateway.requests          -> PARTITION BY RANGE (created_at), monthly
   - billing.credit_transactions  -> PARTITION BY RANGE (created_at), monthly
   - partners.clicks              -> PARTITION BY RANGE (clicked_at), monthly
   - audit.audit_log              -> PARTITION BY RANGE (created_at), monthly
   - analytics.usage_metrics_hourly -> PARTITION BY RANGE (metric_hour), weekly

   Example:
   CREATE TABLE analytics.events (
       ...
   ) PARTITION BY RANGE (created_at);

   CREATE TABLE analytics.events_2026_01 PARTITION OF analytics.events
       FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

3. DATA RETENTION POLICY:
   - analytics.events:              Keep 12 months, archive to cold storage
   - analytics.usage_metrics_hourly: Keep 90 days, roll up to daily
   - ai_gateway.requests:           Keep 6 months, archive to cold storage
   - ai_gateway.response_cache:     Purge expired entries daily
   - ai_gateway.rate_limit_counters: Purge expired windows hourly
   - partners.clicks:               Keep 12 months
   - audit.audit_log:               Keep 7 years (legal requirement)
   - auth.sessions:                 Purge expired sessions weekly

4. INDEXES STRATEGY:
   - Primary lookups: B-tree (default) for equality/range queries
   - Full-text search: GIN indexes with Portuguese text search configuration
   - Array fields: GIN indexes for tags, scopes, etc.
   - Vector search: HNSW index on kb_chunks.embedding for RAG
   - Partial indexes: WHERE clauses for common filters (is_active, status)

5. ENUM EVOLUTION:
   To add new values to an enum type:
     ALTER TYPE user_status ADD VALUE 'banned' AFTER 'suspended';
   Note: Enum values cannot be removed in PostgreSQL. Plan enum values carefully.

6. LGPD DATA ERASURE PROCEDURE:
   When processing an erasure request (auth.erasure_requests):
   a. Anonymize auth.users: Set email='deleted_<hash>@anon.local', full_name='[APAGADO]', etc.
   b. Delete auth.sessions, auth.api_keys for the user
   c. Anonymize chat.messages: Remove content, keep metadata for billing records
   d. Remove auth.user_identities
   e. Remove auth.consent_records (after recording erasure proof)
   f. Anonymize analytics.events: Remove IP, user_agent, set user_id to NULL
   g. Keep billing records (legal requirement) but anonymize PII fields
   h. Update auth.erasure_requests with erased_tables and completion
   i. Log the erasure in audit.audit_log with involves_pii = TRUE

7. SEED DATA:
   After migration, seed the following:
   - billing.plans: Create the 5 plan tiers with correct pricing
   - ai_gateway.providers: Add OpenAI, Anthropic, Google, Mistral, etc.
   - ai_gateway.models: Add available models with credit multipliers
   - ai_gateway.rate_limits: Default rate limits per plan tier
   - auth.feature_flags: Initial feature flags

8. BACKUP STRATEGY:
   - Full backup: Daily via pg_dump or WAL archiving
   - Point-in-time recovery: Enable WAL archiving
   - Cross-region replication: For disaster recovery
   - Test restores: Monthly

9. CONNECTION POOLING:
   - Use PgBouncer in transaction mode
   - Set application_name for each service module
   - Max connections per service: auth=20, chat=30, ai_gateway=40, others=10

10. MONITORING:
    - Enable pg_stat_statements for query analysis
    - Monitor table bloat and schedule VACUUM ANALYZE
    - Alert on long-running queries (>5s)
    - Track index usage and remove unused indexes
    - Monitor connection count and pool utilization
*/
