-- ============================================================================
-- V9: Teams Module Tables
-- ============================================================================

SET search_path TO teams, auth, public;

-- Workspaces
CREATE TABLE teams.workspaces (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    description     TEXT,
    logo_url        TEXT,
    settings        JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_workspace_slug_org UNIQUE (org_id, slug)
);

CREATE INDEX idx_workspaces_org ON teams.workspaces(org_id);
CREATE INDEX idx_workspaces_slug ON teams.workspaces(slug);

CREATE TRIGGER trg_workspaces_updated_at
    BEFORE UPDATE ON teams.workspaces
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Workspace Members
CREATE TABLE teams.workspace_members (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES teams.workspaces(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role            workspace_role NOT NULL DEFAULT 'member',
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    invited_by      UUID REFERENCES auth.users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_workspace_member UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_members_workspace ON teams.workspace_members(workspace_id);
CREATE INDEX idx_workspace_members_user ON teams.workspace_members(user_id);

CREATE TRIGGER trg_workspace_members_updated_at
    BEFORE UPDATE ON teams.workspace_members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Invitations
CREATE TABLE teams.invitations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workspace_id    UUID NOT NULL REFERENCES teams.workspaces(id) ON DELETE CASCADE,
    email           VARCHAR(320) NOT NULL,
    role            workspace_role NOT NULL DEFAULT 'member',
    status          invite_status NOT NULL DEFAULT 'pending',
    invited_by      UUID NOT NULL REFERENCES auth.users(id),
    token           VARCHAR(64) NOT NULL UNIQUE,
    message         TEXT,
    accepted_by     UUID REFERENCES auth.users(id),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '7 days'),
    accepted_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invitations_workspace ON teams.invitations(workspace_id);
CREATE INDEX idx_invitations_email ON teams.invitations(email);
CREATE INDEX idx_invitations_token ON teams.invitations(token);
CREATE INDEX idx_invitations_status ON teams.invitations(status) WHERE status = 'pending';

CREATE TRIGGER trg_invitations_updated_at
    BEFORE UPDATE ON teams.invitations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
