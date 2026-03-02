-- ============================================================================
-- IA AGGREGATOR - Module 7: TEAMS
-- File: 07_teams.sql
-- Purpose: Workspaces, members, team roles, invites
-- ============================================================================

SET search_path TO teams, public;

-- ============================================================================
-- WORKSPACES
-- ============================================================================

CREATE TABLE teams.workspaces (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- Identity
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    description     TEXT,
    logo_url        TEXT,
    -- Settings
    default_model_id UUID REFERENCES ai_gateway.models(id),
    default_persona_id UUID REFERENCES content.personas(id),
    -- Limits (inherited from plan but overridable)
    max_members     INTEGER,
    max_conversations_per_member INTEGER,
    -- Shared resources
    shared_knowledge_base_ids UUID[] DEFAULT ARRAY[]::UUID[],
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    settings        JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_workspace_slug_org UNIQUE (org_id, slug)
);

COMMENT ON TABLE teams.workspaces IS 'Team workspaces within an organization. Provides shared context and settings.';
COMMENT ON COLUMN teams.workspaces.settings IS 'JSON settings: allowed_models, require_persona, content_policy, etc.';

CREATE INDEX idx_workspaces_org ON teams.workspaces(org_id);
CREATE INDEX idx_workspaces_slug ON teams.workspaces(slug);
CREATE INDEX idx_workspaces_active ON teams.workspaces(is_active) WHERE is_active = TRUE;

CREATE TRIGGER trg_workspaces_updated_at
    BEFORE UPDATE ON teams.workspaces
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- WORKSPACE MEMBERS
-- ============================================================================

CREATE TABLE teams.workspace_members (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES teams.workspaces(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    -- Role
    role            workspace_role NOT NULL DEFAULT 'member',
    -- Permissions (fine-grained overrides beyond role)
    permissions     JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- Usage tracking
    credits_used_this_period BIGINT NOT NULL DEFAULT 0,
    credit_limit    BIGINT, -- per-member credit limit (NULL = unlimited from team pool)
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- Membership dates
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    removed_at      TIMESTAMPTZ,
    removed_by      UUID REFERENCES auth.users(id),
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_workspace_member UNIQUE (workspace_id, user_id)
);

COMMENT ON TABLE teams.workspace_members IS 'Workspace membership with roles and per-member credit limits.';
COMMENT ON COLUMN teams.workspace_members.permissions IS 'Fine-grained permissions: {can_share: true, can_use_api: false, allowed_models: [...]}';

CREATE INDEX idx_ws_members_workspace ON teams.workspace_members(workspace_id);
CREATE INDEX idx_ws_members_user ON teams.workspace_members(user_id);
CREATE INDEX idx_ws_members_role ON teams.workspace_members(role);
CREATE INDEX idx_ws_members_active ON teams.workspace_members(is_active) WHERE is_active = TRUE;

CREATE TRIGGER trg_ws_members_updated_at
    BEFORE UPDATE ON teams.workspace_members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- WORKSPACE INVITES
-- ============================================================================

CREATE TABLE teams.workspace_invites (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES teams.workspaces(id) ON DELETE CASCADE,
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- Invite details
    email           VARCHAR(320) NOT NULL,
    role            workspace_role NOT NULL DEFAULT 'member',
    -- Who invited
    invited_by      UUID NOT NULL REFERENCES auth.users(id),
    -- Invite token
    token_hash      TEXT NOT NULL UNIQUE,
    -- Status
    status          invite_status NOT NULL DEFAULT 'pending',
    -- Lifecycle
    expires_at      TIMESTAMPTZ NOT NULL,
    accepted_at     TIMESTAMPTZ,
    accepted_by     UUID REFERENCES auth.users(id), -- may differ from invited email
    declined_at     TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    revoked_by      UUID REFERENCES auth.users(id),
    -- Metadata
    message         TEXT, -- optional personal message
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE teams.workspace_invites IS 'Pending workspace invitations. Token-based, with expiration.';

CREATE INDEX idx_ws_invites_workspace ON teams.workspace_invites(workspace_id);
CREATE INDEX idx_ws_invites_email ON teams.workspace_invites(email);
CREATE INDEX idx_ws_invites_token ON teams.workspace_invites(token_hash);
CREATE INDEX idx_ws_invites_status ON teams.workspace_invites(status) WHERE status = 'pending';

CREATE TRIGGER trg_ws_invites_updated_at
    BEFORE UPDATE ON teams.workspace_invites
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ORGANIZATION MEMBERS (org-level membership, distinct from workspace)
-- ============================================================================

CREATE TABLE teams.org_members (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    -- Role at org level
    role            workspace_role NOT NULL DEFAULT 'member',
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    removed_at      TIMESTAMPTZ,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_org_member UNIQUE (org_id, user_id)
);

COMMENT ON TABLE teams.org_members IS 'Organization-level membership. Users belong to orgs, then optionally to workspaces within.';

CREATE INDEX idx_org_members_org ON teams.org_members(org_id);
CREATE INDEX idx_org_members_user ON teams.org_members(user_id);
CREATE INDEX idx_org_members_active ON teams.org_members(is_active) WHERE is_active = TRUE;

CREATE TRIGGER trg_org_members_updated_at
    BEFORE UPDATE ON teams.org_members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- ORGANIZATION INVITES
-- ============================================================================

CREATE TABLE teams.org_invites (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    email           VARCHAR(320) NOT NULL,
    role            workspace_role NOT NULL DEFAULT 'member',
    invited_by      UUID NOT NULL REFERENCES auth.users(id),
    token_hash      TEXT NOT NULL UNIQUE,
    status          invite_status NOT NULL DEFAULT 'pending',
    expires_at      TIMESTAMPTZ NOT NULL,
    accepted_at     TIMESTAMPTZ,
    accepted_by     UUID REFERENCES auth.users(id),
    message         TEXT,
    -- Auto-add to workspaces upon acceptance
    auto_join_workspace_ids UUID[] DEFAULT ARRAY[]::UUID[],
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE teams.org_invites IS 'Organization-level invitations. Can auto-add to specific workspaces on acceptance.';

CREATE INDEX idx_org_invites_org ON teams.org_invites(org_id);
CREATE INDEX idx_org_invites_email ON teams.org_invites(email);
CREATE INDEX idx_org_invites_token ON teams.org_invites(token_hash);
CREATE INDEX idx_org_invites_status ON teams.org_invites(status) WHERE status = 'pending';

CREATE TRIGGER trg_org_invites_updated_at
    BEFORE UPDATE ON teams.org_invites
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TEAM ACTIVITY LOG
-- ============================================================================

CREATE TABLE teams.activity_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    workspace_id    UUID REFERENCES teams.workspaces(id),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    -- Activity
    action          VARCHAR(100) NOT NULL, -- 'member_added', 'role_changed', 'settings_updated', etc.
    target_type     VARCHAR(50), -- 'member', 'workspace', 'settings', 'invite'
    target_id       UUID,
    -- Details
    details         JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address      INET,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE teams.activity_log IS 'Team/workspace activity audit trail. Tracks membership changes, settings updates, etc.';

CREATE INDEX idx_team_activity_org ON teams.activity_log(org_id);
CREATE INDEX idx_team_activity_workspace ON teams.activity_log(workspace_id);
CREATE INDEX idx_team_activity_user ON teams.activity_log(user_id);
CREATE INDEX idx_team_activity_created ON teams.activity_log(created_at);
