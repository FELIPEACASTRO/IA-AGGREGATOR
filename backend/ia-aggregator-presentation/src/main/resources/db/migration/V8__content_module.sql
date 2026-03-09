-- ============================================================================
-- V8: Content Module Tables
-- ============================================================================

SET search_path TO content, public;

-- Artifacts
CREATE TABLE content.artifacts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    conversation_id UUID,
    title           VARCHAR(500) NOT NULL,
    artifact_type   VARCHAR(50) NOT NULL,
    content         TEXT NOT NULL,
    mime_type       VARCHAR(100) NOT NULL DEFAULT 'text/plain',
    version         INTEGER NOT NULL DEFAULT 1,
    status          VARCHAR(50) NOT NULL DEFAULT 'draft',
    share_token     VARCHAR(64) UNIQUE,
    is_public       BOOLEAN NOT NULL DEFAULT FALSE,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_artifacts_org ON content.artifacts(org_id);
CREATE INDEX idx_artifacts_user ON content.artifacts(created_by);
CREATE INDEX idx_artifacts_conversation ON content.artifacts(conversation_id);
CREATE INDEX idx_artifacts_share ON content.artifacts(share_token) WHERE share_token IS NOT NULL;

CREATE TRIGGER trg_artifacts_updated_at
    BEFORE UPDATE ON content.artifacts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Artifact Versions
CREATE TABLE content.artifact_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    artifact_id     UUID NOT NULL REFERENCES content.artifacts(id) ON DELETE CASCADE,
    version_number  INTEGER NOT NULL,
    content         TEXT NOT NULL,
    change_description TEXT,
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_artifact_version UNIQUE (artifact_id, version_number)
);

CREATE INDEX idx_artifact_versions_artifact ON content.artifact_versions(artifact_id);

-- Assets (prompts, templates, system instructions)
CREATE TABLE content.assets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    created_by      UUID NOT NULL REFERENCES auth.users(id),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    asset_type      VARCHAR(50) NOT NULL,
    scope           VARCHAR(50) NOT NULL DEFAULT 'personal',
    content         TEXT NOT NULL,
    variables       JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    usage_count     BIGINT NOT NULL DEFAULT 0,
    average_rating  NUMERIC(3, 2) NOT NULL DEFAULT 0,
    total_cost_usd  NUMERIC(12, 4) NOT NULL DEFAULT 0,
    success_count   BIGINT NOT NULL DEFAULT 0,
    failure_count   BIGINT NOT NULL DEFAULT 0,
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assets_org ON content.assets(org_id);
CREATE INDEX idx_assets_type ON content.assets(asset_type);
CREATE INDEX idx_assets_scope ON content.assets(scope);
CREATE INDEX idx_assets_published ON content.assets(published) WHERE published = TRUE;

CREATE TRIGGER trg_assets_updated_at
    BEFORE UPDATE ON content.assets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Agents (prebuilt and custom)
CREATE TABLE content.agents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID REFERENCES auth.organizations(id) ON DELETE CASCADE,
    created_by      UUID REFERENCES auth.users(id),
    agent_id        VARCHAR(100) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    icon            VARCHAR(50),
    default_model   VARCHAR(100),
    system_prompt   TEXT,
    capabilities    TEXT[] NOT NULL DEFAULT '{}',
    config          JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_prebuilt     BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    usage_count     BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_agent_id_org UNIQUE (agent_id, org_id)
);

CREATE INDEX idx_agents_org ON content.agents(org_id);
CREATE INDEX idx_agents_prebuilt ON content.agents(is_prebuilt) WHERE is_prebuilt = TRUE;
CREATE INDEX idx_agents_active ON content.agents(is_active) WHERE is_active = TRUE;

CREATE TRIGGER trg_agents_updated_at
    BEFORE UPDATE ON content.agents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed prebuilt agents
INSERT INTO content.agents (agent_id, name, description, category, icon, default_model, capabilities, is_prebuilt) VALUES
    ('chat-general', 'Chat Geral', 'Assistente de uso geral para conversas livres', 'chat', 'MessageSquare', 'gpt-4o-mini', '{CHAT}', TRUE),
    ('chat-research', 'Pesquisa Profunda', 'Pesquisa aprofundada com múltiplas fontes', 'chat', 'Search', 'sonar-pro', '{CHAT,SEARCH}', TRUE),
    ('codex-ask', 'Codex Ask', 'Tire dúvidas sobre código e arquitetura', 'codex', 'Code', 'claude-3-5-haiku', '{CHAT,CODE}', TRUE),
    ('codex-code', 'Codex Code', 'Gere e edite código automaticamente', 'codex', 'Terminal', 'gpt-4o-mini', '{CHAT,CODE}', TRUE),
    ('codex-review', 'Codex Review', 'Revisão automática de pull requests', 'codex', 'GitPullRequest', 'claude-3-5-haiku', '{CHAT,CODE}', TRUE),
    ('creative-image', 'Geração de Imagens', 'Crie imagens a partir de descrições', 'creative', 'Image', 'dall-e-3', '{IMAGE_GENERATION}', TRUE),
    ('creative-video', 'Geração de Vídeo', 'Crie vídeos curtos com IA', 'creative', 'Video', 'gen4_turbo', '{VIDEO_GENERATION}', TRUE),
    ('speech-transcribe', 'Transcrição de Áudio', 'Transcreva áudio para texto', 'speech', 'Mic', 'whisper-1', '{AUDIO_TRANSCRIPTION}', TRUE),
    ('speech-generate', 'Síntese de Voz', 'Gere áudio a partir de texto', 'speech', 'Volume2', 'tts-1', '{AUDIO_TTS}', TRUE);

-- Prompt Templates
CREATE TABLE content.prompt_templates (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID REFERENCES auth.organizations(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    category        template_category NOT NULL DEFAULT 'custom',
    prompt_type     prompt_type NOT NULL DEFAULT 'user',
    content         TEXT NOT NULL,
    variables       JSONB NOT NULL DEFAULT '[]'::jsonb,
    example_output  TEXT,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    usage_count     BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prompt_templates_org ON content.prompt_templates(org_id);
CREATE INDEX idx_prompt_templates_category ON content.prompt_templates(category);
CREATE INDEX idx_prompt_templates_system ON content.prompt_templates(is_system) WHERE is_system = TRUE;

CREATE TRIGGER trg_prompt_templates_updated_at
    BEFORE UPDATE ON content.prompt_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed prompt categories
INSERT INTO content.prompt_templates (name, description, category, content, is_system) VALUES
    ('Escrita Criativa', 'Ajuda com redação, artigos e textos criativos', 'writing', 'Você é um assistente de escrita criativa. Ajude o usuário a escrever textos claros, envolventes e bem estruturados.', TRUE),
    ('Aprendizado', 'Explica conceitos de forma didática', 'education', 'Você é um tutor paciente e didático. Explique conceitos de forma clara usando analogias e exemplos práticos.', TRUE),
    ('Código', 'Assistente de programação e desenvolvimento', 'coding', 'Você é um engenheiro de software experiente. Ajude com código limpo, boas práticas e debugging.', TRUE),
    ('Estratégia', 'Análise estratégica e tomada de decisão', 'business', 'Você é um consultor estratégico. Ajude a analisar cenários, identificar oportunidades e estruturar planos de ação.', TRUE),
    ('Exploração', 'Pesquisa e descoberta de informações', 'analysis', 'Você é um pesquisador meticuloso. Ajude a explorar temas em profundidade com fontes e análise crítica.', TRUE);

-- Knowledge Bases
CREATE TABLE content.knowledge_bases (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    embedding_model VARCHAR(100) NOT NULL DEFAULT 'text-embedding-3-small',
    chunk_size      INTEGER NOT NULL DEFAULT 512,
    chunk_overlap   INTEGER NOT NULL DEFAULT 50,
    document_count  INTEGER NOT NULL DEFAULT 0,
    total_chunks    INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kb_org ON content.knowledge_bases(org_id);

CREATE TRIGGER trg_knowledge_bases_updated_at
    BEFORE UPDATE ON content.knowledge_bases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Knowledge Documents
CREATE TABLE content.knowledge_documents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    collection_id   UUID NOT NULL REFERENCES content.knowledge_bases(id) ON DELETE CASCADE,
    title           VARCHAR(500) NOT NULL,
    source_url      TEXT,
    mime_type       VARCHAR(100) NOT NULL DEFAULT 'text/plain',
    size_bytes      BIGINT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    chunk_count     INTEGER NOT NULL DEFAULT 0,
    chunking_strategy VARCHAR(50) NOT NULL DEFAULT 'FIXED_SIZE',
    embedding_model VARCHAR(100),
    error_message   TEXT,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kb_docs_org ON content.knowledge_documents(org_id);
CREATE INDEX idx_kb_docs_collection ON content.knowledge_documents(collection_id);
CREATE INDEX idx_kb_docs_status ON content.knowledge_documents(status);

CREATE TRIGGER trg_knowledge_documents_updated_at
    BEFORE UPDATE ON content.knowledge_documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Knowledge Chunks
CREATE TABLE content.knowledge_chunks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    knowledge_base_id UUID NOT NULL REFERENCES content.knowledge_bases(id) ON DELETE CASCADE,
    document_id     UUID NOT NULL,
    chunk_index     INTEGER NOT NULL,
    content         TEXT NOT NULL,
    embedding       vector(1536),
    source_type     kb_source_type NOT NULL DEFAULT 'manual_entry',
    source_ref      TEXT,
    status          chunk_status NOT NULL DEFAULT 'pending',
    token_count     INTEGER,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chunks_kb ON content.knowledge_chunks(knowledge_base_id);
CREATE INDEX idx_chunks_document ON content.knowledge_chunks(document_id);
CREATE INDEX idx_chunks_status ON content.knowledge_chunks(status);
CREATE INDEX idx_chunks_embedding ON content.knowledge_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
