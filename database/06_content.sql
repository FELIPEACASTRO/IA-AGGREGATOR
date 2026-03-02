-- ============================================================================
-- IA AGGREGATOR - Module 6: CONTENT
-- File: 06_content.sql
-- Purpose: Personas, prompts, templates, knowledge bases (RAG), embeddings
-- ============================================================================

SET search_path TO content, public;

-- ============================================================================
-- PERSONAS
-- ============================================================================

CREATE TABLE content.personas (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Ownership
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID REFERENCES auth.organizations(id),
    -- Identity
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    description     TEXT,
    avatar_url      TEXT,
    -- Persona configuration
    system_prompt   TEXT NOT NULL,
    greeting_message TEXT, -- initial message when conversation starts
    -- Model preferences
    preferred_model_id UUID REFERENCES ai_gateway.models(id),
    temperature     NUMERIC(3,2) DEFAULT 0.7 CHECK (temperature BETWEEN 0 AND 2),
    max_tokens      INTEGER,
    top_p           NUMERIC(3,2),
    -- Visibility
    visibility      persona_visibility NOT NULL DEFAULT 'private',
    -- Usage stats (denormalized)
    use_count       BIGINT NOT NULL DEFAULT 0,
    like_count      INTEGER NOT NULL DEFAULT 0,
    -- Marketplace
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    marketplace_price_credits BIGINT, -- NULL = free
    -- Categories / tags
    category        template_category,
    tags            TEXT[] DEFAULT ARRAY[]::TEXT[],
    -- Versioning
    version         INTEGER NOT NULL DEFAULT 1,
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_approved     BOOLEAN NOT NULL DEFAULT FALSE, -- for marketplace/public
    approved_at     TIMESTAMPTZ,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_persona_slug_org UNIQUE (org_id, slug)
);

COMMENT ON TABLE content.personas IS 'AI personas with custom system prompts, model preferences, and marketplace publishing.';
COMMENT ON COLUMN content.personas.system_prompt IS 'The system prompt that defines this persona behavior and personality.';
COMMENT ON COLUMN content.personas.visibility IS 'private=creator only, workspace=team, public=listed in marketplace.';

-- Add FK from chat.conversations to personas
ALTER TABLE chat.conversations
    ADD CONSTRAINT fk_conversations_persona
    FOREIGN KEY (persona_id) REFERENCES content.personas(id);

CREATE INDEX idx_personas_creator ON content.personas(created_by);
CREATE INDEX idx_personas_org ON content.personas(org_id);
CREATE INDEX idx_personas_visibility ON content.personas(visibility);
CREATE INDEX idx_personas_category ON content.personas(category);
CREATE INDEX idx_personas_tags ON content.personas USING GIN(tags);
CREATE INDEX idx_personas_marketplace ON content.personas(is_active, visibility, is_approved)
    WHERE visibility = 'public' AND is_active = TRUE AND is_approved = TRUE;
CREATE INDEX idx_personas_featured ON content.personas(is_featured) WHERE is_featured = TRUE;

CREATE TRIGGER trg_personas_updated_at
    BEFORE UPDATE ON content.personas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- PERSONA VERSIONS (history of system prompt changes)
-- ============================================================================

CREATE TABLE content.persona_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    persona_id      UUID NOT NULL REFERENCES content.personas(id) ON DELETE CASCADE,
    version         INTEGER NOT NULL,
    system_prompt   TEXT NOT NULL,
    greeting_message TEXT,
    change_notes    TEXT,
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_persona_version UNIQUE (persona_id, version)
);

COMMENT ON TABLE content.persona_versions IS 'Version history for persona system prompts. Immutable records.';

CREATE INDEX idx_persona_versions_persona ON content.persona_versions(persona_id);

-- ============================================================================
-- PROMPT TEMPLATES
-- ============================================================================

CREATE TABLE content.prompt_templates (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Ownership
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID REFERENCES auth.organizations(id),
    -- Identity
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    description     TEXT,
    -- Template content
    prompt_type     prompt_type NOT NULL DEFAULT 'user',
    content         TEXT NOT NULL, -- supports {{variable}} placeholders
    -- Variables (defined placeholders)
    variables       JSONB NOT NULL DEFAULT '[]'::jsonb,
    -- e.g., [{"name": "topic", "description": "The topic to write about", "required": true, "default": ""}]
    -- Category
    category        template_category NOT NULL DEFAULT 'custom',
    tags            TEXT[] DEFAULT ARRAY[]::TEXT[],
    -- Visibility
    visibility      persona_visibility NOT NULL DEFAULT 'private',
    -- Usage stats
    use_count       BIGINT NOT NULL DEFAULT 0,
    like_count      INTEGER NOT NULL DEFAULT 0,
    -- Marketplace
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    -- Model recommendation
    recommended_model_id UUID REFERENCES ai_gateway.models(id),
    recommended_temperature NUMERIC(3,2),
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_approved     BOOLEAN NOT NULL DEFAULT FALSE,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_template_slug_org UNIQUE (org_id, slug)
);

COMMENT ON TABLE content.prompt_templates IS 'Reusable prompt templates with variable placeholders. Publishable to marketplace.';
COMMENT ON COLUMN content.prompt_templates.content IS 'Template text with {{variable_name}} placeholders for dynamic content.';
COMMENT ON COLUMN content.prompt_templates.variables IS 'JSON array defining template variables: [{name, description, required, default, type}].';

CREATE INDEX idx_templates_creator ON content.prompt_templates(created_by);
CREATE INDEX idx_templates_org ON content.prompt_templates(org_id);
CREATE INDEX idx_templates_category ON content.prompt_templates(category);
CREATE INDEX idx_templates_visibility ON content.prompt_templates(visibility);
CREATE INDEX idx_templates_tags ON content.prompt_templates USING GIN(tags);
CREATE INDEX idx_templates_marketplace ON content.prompt_templates(is_active, visibility, is_approved)
    WHERE visibility = 'public' AND is_active = TRUE AND is_approved = TRUE;

CREATE TRIGGER trg_templates_updated_at
    BEFORE UPDATE ON content.prompt_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- PROMPT CHAINS (multi-step prompt sequences)
-- ============================================================================

CREATE TABLE content.prompt_chains (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID REFERENCES auth.organizations(id),
    -- Identity
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    -- Configuration
    steps           JSONB NOT NULL DEFAULT '[]'::jsonb,
    -- [{order: 1, template_id: uuid, model_id: uuid, output_variable: "result1"}, ...]
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    visibility      persona_visibility NOT NULL DEFAULT 'private',
    use_count       BIGINT NOT NULL DEFAULT 0,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE content.prompt_chains IS 'Multi-step prompt chains where output of one step feeds into the next.';
COMMENT ON COLUMN content.prompt_chains.steps IS 'JSON array of chain steps: [{order, template_id, model_id, output_variable, config}].';

CREATE INDEX idx_chains_creator ON content.prompt_chains(created_by);
CREATE INDEX idx_chains_org ON content.prompt_chains(org_id);

CREATE TRIGGER trg_chains_updated_at
    BEFORE UPDATE ON content.prompt_chains
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- KNOWLEDGE BASES (RAG)
-- ============================================================================

CREATE TABLE content.knowledge_bases (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Ownership
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- Identity
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    -- Configuration
    embedding_model_id UUID REFERENCES ai_gateway.models(id), -- model used for embeddings
    chunk_size      INTEGER NOT NULL DEFAULT 512, -- tokens per chunk
    chunk_overlap   INTEGER NOT NULL DEFAULT 50, -- overlap between chunks
    -- Statistics
    total_documents INTEGER NOT NULL DEFAULT 0,
    total_chunks    INTEGER NOT NULL DEFAULT 0,
    total_tokens    BIGINT NOT NULL DEFAULT 0,
    total_size_bytes BIGINT NOT NULL DEFAULT 0,
    -- Processing status
    processing_status kb_processing_status NOT NULL DEFAULT 'pending',
    last_synced_at  TIMESTAMPTZ,
    -- Limits
    max_documents   INTEGER DEFAULT 100,
    max_size_bytes  BIGINT DEFAULT 104857600, -- 100MB default
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    settings        JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE content.knowledge_bases IS 'RAG knowledge bases. Collections of documents chunked and embedded for semantic retrieval.';
COMMENT ON COLUMN content.knowledge_bases.chunk_size IS 'Number of tokens per document chunk for embedding.';

CREATE INDEX idx_kb_creator ON content.knowledge_bases(created_by);
CREATE INDEX idx_kb_org ON content.knowledge_bases(org_id);
CREATE INDEX idx_kb_status ON content.knowledge_bases(processing_status);

CREATE TRIGGER trg_kb_updated_at
    BEFORE UPDATE ON content.knowledge_bases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- KNOWLEDGE BASE DOCUMENTS (source documents)
-- ============================================================================

CREATE TABLE content.kb_documents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    knowledge_base_id UUID NOT NULL REFERENCES content.knowledge_bases(id) ON DELETE CASCADE,
    -- Source
    source_type     kb_source_type NOT NULL,
    source_url      TEXT, -- for url_crawl, api_sync
    -- File details
    file_name       VARCHAR(500),
    file_type       VARCHAR(50), -- pdf, docx, txt, md, html
    mime_type       VARCHAR(100),
    file_size_bytes BIGINT,
    -- Storage
    storage_key     TEXT, -- S3/R2 object key for uploaded files
    storage_bucket  VARCHAR(100),
    -- Content
    raw_content     TEXT, -- extracted text content
    content_hash    TEXT, -- SHA-256 of content for dedup
    -- Processing
    processing_status kb_processing_status NOT NULL DEFAULT 'pending',
    chunk_count     INTEGER NOT NULL DEFAULT 0,
    token_count     BIGINT NOT NULL DEFAULT 0,
    processing_error TEXT,
    processed_at    TIMESTAMPTZ,
    -- Sync (for api_sync and url_crawl)
    last_synced_at  TIMESTAMPTZ,
    sync_frequency  VARCHAR(20), -- 'hourly', 'daily', 'weekly', 'manual'
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE content.kb_documents IS 'Source documents within a knowledge base. Supports file upload, URL crawl, and API sync.';

CREATE INDEX idx_kb_docs_kb ON content.kb_documents(knowledge_base_id);
CREATE INDEX idx_kb_docs_status ON content.kb_documents(processing_status);
CREATE INDEX idx_kb_docs_hash ON content.kb_documents(content_hash);

CREATE TRIGGER trg_kb_docs_updated_at
    BEFORE UPDATE ON content.kb_documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- KNOWLEDGE BASE CHUNKS (embedded document fragments)
-- ============================================================================

CREATE TABLE content.kb_chunks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id     UUID NOT NULL REFERENCES content.kb_documents(id) ON DELETE CASCADE,
    knowledge_base_id UUID NOT NULL REFERENCES content.knowledge_bases(id) ON DELETE CASCADE,
    -- Chunk content
    content         TEXT NOT NULL,
    token_count     INTEGER NOT NULL,
    -- Position in document
    chunk_index     INTEGER NOT NULL, -- sequential position
    start_offset    INTEGER, -- character offset in source document
    end_offset      INTEGER,
    -- Embedding vector (using pgvector)
    embedding       vector(1536), -- OpenAI ada-002 dimension; adjust for other models
    -- Status
    status          chunk_status NOT NULL DEFAULT 'pending',
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb, -- heading, page_number, etc.
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE content.kb_chunks IS 'Document chunks with vector embeddings for RAG retrieval. Uses pgvector for similarity search.';
COMMENT ON COLUMN content.kb_chunks.embedding IS 'Vector embedding (1536-dim for OpenAI ada-002). Indexed with IVFFlat or HNSW.';

CREATE INDEX idx_kb_chunks_document ON content.kb_chunks(document_id);
CREATE INDEX idx_kb_chunks_kb ON content.kb_chunks(knowledge_base_id);
CREATE INDEX idx_kb_chunks_status ON content.kb_chunks(status);
-- HNSW index for fast approximate nearest neighbor search
CREATE INDEX idx_kb_chunks_embedding ON content.kb_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ============================================================================
-- KNOWLEDGE BASE - CONVERSATION LINK
-- ============================================================================

CREATE TABLE content.conversation_knowledge_bases (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES chat.conversations(id) ON DELETE CASCADE,
    knowledge_base_id UUID NOT NULL REFERENCES content.knowledge_bases(id) ON DELETE CASCADE,
    -- RAG settings for this conversation
    top_k           INTEGER NOT NULL DEFAULT 5, -- number of chunks to retrieve
    similarity_threshold NUMERIC(3,2) DEFAULT 0.7, -- minimum similarity score
    -- Metadata
    attached_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    attached_by     UUID NOT NULL REFERENCES auth.users(id),

    CONSTRAINT uq_conv_kb UNIQUE (conversation_id, knowledge_base_id)
);

COMMENT ON TABLE content.conversation_knowledge_bases IS 'Links knowledge bases to conversations for RAG-enhanced responses.';

CREATE INDEX idx_conv_kb_conversation ON content.conversation_knowledge_bases(conversation_id);
CREATE INDEX idx_conv_kb_kb ON content.conversation_knowledge_bases(knowledge_base_id);

-- ============================================================================
-- RAG RETRIEVAL LOG (track what was retrieved for each message)
-- ============================================================================

CREATE TABLE content.rag_retrievals (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id      UUID NOT NULL REFERENCES chat.messages(id) ON DELETE CASCADE,
    knowledge_base_id UUID NOT NULL REFERENCES content.knowledge_bases(id),
    -- Retrieved chunks
    chunk_id        UUID NOT NULL REFERENCES content.kb_chunks(id),
    similarity_score NUMERIC(5,4) NOT NULL,
    rank            INTEGER NOT NULL, -- position in retrieval results
    -- Was it actually used in the response?
    was_used        BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE content.rag_retrievals IS 'Log of chunks retrieved for each RAG-enhanced message. For debugging and quality analysis.';

CREATE INDEX idx_rag_retrievals_message ON content.rag_retrievals(message_id);
CREATE INDEX idx_rag_retrievals_kb ON content.rag_retrievals(knowledge_base_id);
CREATE INDEX idx_rag_retrievals_chunk ON content.rag_retrievals(chunk_id);

-- ============================================================================
-- CONTENT LIKES / RATINGS (for marketplace)
-- ============================================================================

CREATE TABLE content.content_ratings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    -- What is being rated (polymorphic)
    content_type    VARCHAR(20) NOT NULL, -- 'persona', 'template'
    content_id      UUID NOT NULL,
    -- Rating
    rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text     TEXT,
    -- Status
    is_published    BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_content_rating UNIQUE (user_id, content_type, content_id)
);

COMMENT ON TABLE content.content_ratings IS 'User ratings and reviews for marketplace personas and templates.';

CREATE INDEX idx_content_ratings_content ON content.content_ratings(content_type, content_id);
CREATE INDEX idx_content_ratings_user ON content.content_ratings(user_id);

CREATE TRIGGER trg_content_ratings_updated_at
    BEFORE UPDATE ON content.content_ratings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
