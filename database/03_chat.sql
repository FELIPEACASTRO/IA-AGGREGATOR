-- ============================================================================
-- IA AGGREGATOR - Module 3: CHAT
-- File: 03_chat.sql
-- Purpose: Conversations, messages, attachments, forks, shared links
-- ============================================================================

SET search_path TO chat, public;

-- ============================================================================
-- CONVERSATIONS
-- ============================================================================

CREATE TABLE chat.conversations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- Conversation details
    title           VARCHAR(500), -- auto-generated or user-defined
    summary         TEXT, -- AI-generated summary
    status          conversation_status NOT NULL DEFAULT 'active',
    -- Model preference for this conversation
    model_id        UUID, -- FK to ai_gateway.models, added later
    -- Persona / system prompt override
    persona_id      UUID, -- FK to content.personas, added later
    system_prompt   TEXT, -- custom system prompt (overrides persona)
    -- Fork tracking
    forked_from_id  UUID REFERENCES chat.conversations(id),
    forked_from_message_id UUID, -- the message where the fork starts
    fork_depth      INTEGER NOT NULL DEFAULT 0,
    -- Settings for this conversation
    temperature     NUMERIC(3,2) DEFAULT 0.7 CHECK (temperature BETWEEN 0 AND 2),
    max_tokens      INTEGER,
    top_p           NUMERIC(3,2),
    -- Pinned / favorite
    is_pinned       BOOLEAN NOT NULL DEFAULT FALSE,
    is_favorite     BOOLEAN NOT NULL DEFAULT FALSE,
    -- Folder/organization
    folder_id       UUID, -- FK to chat.folders
    -- Statistics
    message_count   INTEGER NOT NULL DEFAULT 0,
    total_tokens_used INTEGER NOT NULL DEFAULT 0,
    total_credits_used BIGINT NOT NULL DEFAULT 0,
    -- Metadata
    tags            TEXT[] DEFAULT ARRAY[]::TEXT[],
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_message_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    archived_at     TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ -- soft delete
);

COMMENT ON TABLE chat.conversations IS 'Chat conversations (threads). Supports forking, folders, personas, and per-conversation model selection.';
COMMENT ON COLUMN chat.conversations.forked_from_id IS 'If this conversation was forked from another, reference to the source conversation.';
COMMENT ON COLUMN chat.conversations.system_prompt IS 'Custom system prompt for this conversation. Overrides persona system_prompt if both are set.';

CREATE INDEX idx_conversations_user ON chat.conversations(user_id);
CREATE INDEX idx_conversations_org ON chat.conversations(org_id);
CREATE INDEX idx_conversations_status ON chat.conversations(status);
CREATE INDEX idx_conversations_folder ON chat.conversations(folder_id);
CREATE INDEX idx_conversations_forked ON chat.conversations(forked_from_id) WHERE forked_from_id IS NOT NULL;
CREATE INDEX idx_conversations_last_msg ON chat.conversations(user_id, last_message_at DESC);
CREATE INDEX idx_conversations_tags ON chat.conversations USING GIN(tags);
CREATE INDEX idx_conversations_deleted ON chat.conversations(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_conversations_created ON chat.conversations(created_at);

CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON chat.conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- CONVERSATION FOLDERS
-- ============================================================================

CREATE TABLE chat.folders (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    name            VARCHAR(200) NOT NULL,
    color           VARCHAR(7), -- hex color code
    icon            VARCHAR(50), -- icon identifier
    parent_id       UUID REFERENCES chat.folders(id), -- nested folders
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE chat.folders IS 'User-created folders for organizing conversations. Supports nesting.';

CREATE INDEX idx_folders_user ON chat.folders(user_id);
CREATE INDEX idx_folders_parent ON chat.folders(parent_id);

CREATE TRIGGER trg_folders_updated_at
    BEFORE UPDATE ON chat.folders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add FK from conversations to folders now that folders table exists
ALTER TABLE chat.conversations
    ADD CONSTRAINT fk_conversations_folder
    FOREIGN KEY (folder_id) REFERENCES chat.folders(id) ON DELETE SET NULL;

-- ============================================================================
-- MESSAGES
-- ============================================================================

CREATE TABLE chat.messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES chat.conversations(id) ON DELETE CASCADE,
    -- Message details
    role            message_role NOT NULL,
    content         TEXT, -- message text content (can be NULL for tool calls)
    status          message_status NOT NULL DEFAULT 'completed',
    -- Model used for this specific message (assistant messages)
    model_id        UUID, -- FK to ai_gateway.models
    model_name      VARCHAR(100), -- denormalized for quick display
    -- Token usage
    tokens_input    INTEGER DEFAULT 0,
    tokens_output   INTEGER DEFAULT 0,
    tokens_total    INTEGER DEFAULT 0,
    -- Credit cost
    credits_used    BIGINT DEFAULT 0,
    credit_multiplier NUMERIC(6,4) DEFAULT 1.0,
    -- Timing
    response_time_ms INTEGER, -- time to first token
    total_time_ms   INTEGER, -- total generation time
    -- Streaming
    is_streaming    BOOLEAN NOT NULL DEFAULT FALSE,
    stream_completed_at TIMESTAMPTZ,
    -- Error handling
    error_code      VARCHAR(50),
    error_message   TEXT,
    -- Tool calls (function calling)
    tool_calls      JSONB, -- [{id, function: {name, arguments}, type}]
    tool_call_id    VARCHAR(255), -- for tool response messages
    -- Parent message for branching within conversation
    parent_message_id UUID REFERENCES chat.messages(id),
    -- Position in conversation
    sequence_number INTEGER NOT NULL,
    -- Feedback
    user_rating     SMALLINT CHECK (user_rating BETWEEN -1 AND 1), -- -1=bad, 0=neutral, 1=good
    user_feedback   TEXT,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    edited_at       TIMESTAMPTZ,

    CONSTRAINT msg_content_or_tool CHECK (content IS NOT NULL OR tool_calls IS NOT NULL)
);

COMMENT ON TABLE chat.messages IS 'Individual messages within a conversation. Tracks token usage, costs, streaming, and feedback.';
COMMENT ON COLUMN chat.messages.tool_calls IS 'JSON array of tool/function calls made by the assistant model.';
COMMENT ON COLUMN chat.messages.parent_message_id IS 'For message branching: points to the message this is a response variant of.';
COMMENT ON COLUMN chat.messages.sequence_number IS 'Sequential ordering within the conversation for display.';

CREATE INDEX idx_messages_conversation ON chat.messages(conversation_id, sequence_number);
CREATE INDEX idx_messages_role ON chat.messages(role);
CREATE INDEX idx_messages_model ON chat.messages(model_id);
CREATE INDEX idx_messages_parent ON chat.messages(parent_message_id) WHERE parent_message_id IS NOT NULL;
CREATE INDEX idx_messages_created ON chat.messages(created_at);
CREATE INDEX idx_messages_status ON chat.messages(status);
-- Full-text search on message content
CREATE INDEX idx_messages_content_search ON chat.messages USING GIN(to_tsvector('portuguese', coalesce(content, '')));

-- ============================================================================
-- MESSAGE ATTACHMENTS
-- ============================================================================

CREATE TABLE chat.message_attachments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id      UUID NOT NULL REFERENCES chat.messages(id) ON DELETE CASCADE,
    -- File details
    file_name       VARCHAR(500) NOT NULL,
    file_type       attachment_type NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    -- Storage
    storage_key     TEXT NOT NULL, -- S3/R2 object key
    storage_bucket  VARCHAR(100) NOT NULL,
    thumbnail_key   TEXT, -- for images/videos
    -- Processing
    is_processed    BOOLEAN NOT NULL DEFAULT FALSE,
    extracted_text  TEXT, -- OCR or document text extraction
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb, -- dimensions, duration, etc.
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE chat.message_attachments IS 'Files attached to messages: images, documents, audio. Stored in object storage.';

CREATE INDEX idx_attachments_message ON chat.message_attachments(message_id);
CREATE INDEX idx_attachments_type ON chat.message_attachments(file_type);

-- ============================================================================
-- SHARED CONVERSATION LINKS
-- ============================================================================

CREATE TABLE chat.shared_links (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES chat.conversations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    -- Link details
    share_code      VARCHAR(20) NOT NULL UNIQUE DEFAULT generate_short_id(12),
    visibility      share_visibility NOT NULL DEFAULT 'unlisted',
    -- What to share
    title_override  VARCHAR(500), -- optional title for shared view
    include_up_to_message_id UUID REFERENCES chat.messages(id), -- share up to this message
    -- Protection
    password_hash   TEXT, -- optional password protection
    -- Expiration
    expires_at      TIMESTAMPTZ,
    max_views       INTEGER,
    view_count      INTEGER NOT NULL DEFAULT 0,
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_viewed_at  TIMESTAMPTZ
);

COMMENT ON TABLE chat.shared_links IS 'Shareable links to conversations. Supports unlisted/public, password protection, expiration.';

CREATE INDEX idx_shared_links_conversation ON chat.shared_links(conversation_id);
CREATE INDEX idx_shared_links_code ON chat.shared_links(share_code);
CREATE INDEX idx_shared_links_user ON chat.shared_links(user_id);
CREATE INDEX idx_shared_links_active ON chat.shared_links(is_active) WHERE is_active = TRUE;

CREATE TRIGGER trg_shared_links_updated_at
    BEFORE UPDATE ON chat.shared_links
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- CONVERSATION PARTICIPANTS (for team shared conversations)
-- ============================================================================

CREATE TABLE chat.conversation_participants (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES chat.conversations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    -- Permissions
    can_write       BOOLEAN NOT NULL DEFAULT FALSE, -- can send messages
    can_manage      BOOLEAN NOT NULL DEFAULT FALSE, -- can rename, archive, share
    -- Tracking
    last_read_message_id UUID REFERENCES chat.messages(id),
    last_read_at    TIMESTAMPTZ,
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at         TIMESTAMPTZ,

    CONSTRAINT uq_conversation_participant UNIQUE (conversation_id, user_id)
);

COMMENT ON TABLE chat.conversation_participants IS 'For team/shared conversations: tracks who has access and their read state.';

CREATE INDEX idx_conv_participants_conversation ON chat.conversation_participants(conversation_id);
CREATE INDEX idx_conv_participants_user ON chat.conversation_participants(user_id);

-- ============================================================================
-- SAVED PROMPTS / QUICK ACTIONS (per user)
-- ============================================================================

CREATE TABLE chat.saved_prompts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- Prompt details
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    -- Organization
    category        VARCHAR(100),
    is_favorite     BOOLEAN NOT NULL DEFAULT FALSE,
    use_count       INTEGER NOT NULL DEFAULT 0,
    -- Metadata
    sort_order      INTEGER DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE chat.saved_prompts IS 'User-saved quick prompts / snippets for reuse in conversations.';

CREATE INDEX idx_saved_prompts_user ON chat.saved_prompts(user_id);

CREATE TRIGGER trg_saved_prompts_updated_at
    BEFORE UPDATE ON chat.saved_prompts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
