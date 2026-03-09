-- ============================================================================
-- V6: Chat Module Tables
-- ============================================================================

SET search_path TO chat, auth, public;

-- Conversations
CREATE TABLE chat.conversations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL DEFAULT 'Nova Conversa',
    status          conversation_status NOT NULL DEFAULT 'active',
    model           VARCHAR(100),
    pinned          BOOLEAN NOT NULL DEFAULT FALSE,
    message_count   INTEGER NOT NULL DEFAULT 0,
    total_tokens    BIGINT NOT NULL DEFAULT 0,
    last_message_at TIMESTAMPTZ,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_conversations_user ON chat.conversations(user_id);
CREATE INDEX idx_conversations_org ON chat.conversations(org_id);
CREATE INDEX idx_conversations_status ON chat.conversations(status) WHERE status = 'active';
CREATE INDEX idx_conversations_updated ON chat.conversations(updated_at DESC);
CREATE INDEX idx_conversations_pinned ON chat.conversations(user_id, pinned) WHERE pinned = TRUE;

CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON chat.conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Messages
CREATE TABLE chat.messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES chat.conversations(id) ON DELETE CASCADE,
    role            message_role NOT NULL,
    content         TEXT NOT NULL,
    status          message_status NOT NULL DEFAULT 'completed',
    model_used      VARCHAR(100),
    provider_used   VARCHAR(100),
    fallback_used   BOOLEAN NOT NULL DEFAULT FALSE,
    attempts        INTEGER NOT NULL DEFAULT 1,
    input_tokens    INTEGER,
    output_tokens   INTEGER,
    cost_usd        NUMERIC(12, 8),
    latency_ms      INTEGER,
    parent_id       UUID REFERENCES chat.messages(id),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation ON chat.messages(conversation_id, created_at);
CREATE INDEX idx_messages_role ON chat.messages(conversation_id, role);

-- Attachments
CREATE TABLE chat.attachments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id      UUID NOT NULL REFERENCES chat.messages(id) ON DELETE CASCADE,
    attachment_type attachment_type NOT NULL,
    filename        VARCHAR(500),
    mime_type       VARCHAR(100),
    size_bytes      BIGINT,
    storage_url     TEXT NOT NULL,
    thumbnail_url   TEXT,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachments_message ON chat.attachments(message_id);

-- Conversation Shares
CREATE TABLE chat.conversation_shares (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES chat.conversations(id) ON DELETE CASCADE,
    shared_by       UUID NOT NULL REFERENCES auth.users(id),
    share_token     VARCHAR(64) NOT NULL UNIQUE,
    visibility      share_visibility NOT NULL DEFAULT 'unlisted',
    expires_at      TIMESTAMPTZ,
    view_count      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shares_conversation ON chat.conversation_shares(conversation_id);
CREATE INDEX idx_shares_token ON chat.conversation_shares(share_token);
