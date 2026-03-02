-- ============================================================================
-- IA AGGREGATOR - Seed Data
-- File: 10_seed_data.sql
-- Purpose: Initial seed data for plans, models, rate limits
-- ============================================================================

-- ============================================================================
-- PLANS (5 pricing tiers)
-- ============================================================================

INSERT INTO billing.plans (name, slug, tier, description, price_monthly_cents, price_yearly_cents, is_per_seat, monthly_credits, credit_rollover_enabled, max_conversations_per_day, max_messages_per_conversation, max_tokens_per_request, max_file_upload_mb, max_knowledge_bases, max_api_keys, allowed_model_tiers, features, trial_days, sort_order) VALUES

-- Free: R$0/month, 300 credits
('Gratuito', 'free', 'free', 'Explore a plataforma com acesso limitado aos modelos basicos.',
 0, 0, FALSE, 300, FALSE,
 10, 50, 4096, 5, 1, 1,
 ARRAY['basic'],
 '["basic_models", "web_chat"]'::jsonb,
 0, 1),

-- Starter: R$39/month, 1000 credits
('Starter', 'starter', 'starter', 'Para uso pessoal com acesso a modelos intermediarios.',
 3900, 39000, FALSE, 1000, FALSE,
 50, 200, 8192, 10, 2, 3,
 ARRAY['basic', 'standard'],
 '["basic_models", "standard_models", "web_chat", "api_access", "conversation_sharing", "prompt_templates"]'::jsonb,
 7, 2),

-- Pro: R$99/month, 4000 credits
('Pro', 'pro', 'pro', 'Para profissionais com acesso a todos os modelos premium.',
 9900, 99000, FALSE, 4000, TRUE,
 NULL, NULL, 32768, 25, 5, 10,
 ARRAY['basic', 'standard', 'premium'],
 '["basic_models", "standard_models", "premium_models", "web_chat", "api_access", "conversation_sharing", "prompt_templates", "personas", "knowledge_bases", "priority_support", "advanced_analytics"]'::jsonb,
 14, 3),

-- Team: R$49/seat/month
('Team', 'team', 'team', 'Para equipes com gestao centralizada e controle de acesso.',
 4900, 49000, TRUE, 2000, TRUE,
 NULL, NULL, 32768, 50, 10, 20,
 ARRAY['basic', 'standard', 'premium'],
 '["basic_models", "standard_models", "premium_models", "web_chat", "api_access", "conversation_sharing", "prompt_templates", "personas", "knowledge_bases", "priority_support", "advanced_analytics", "team_management", "workspace_sharing", "audit_log", "sso"]'::jsonb,
 14, 4),

-- Enterprise: Custom pricing
('Enterprise', 'enterprise', 'enterprise', 'Solucao personalizada para grandes organizacoes.',
 0, 0, TRUE, 0, TRUE,
 NULL, NULL, 128000, 100, 50, 100,
 ARRAY['basic', 'standard', 'premium', 'enterprise'],
 '["basic_models", "standard_models", "premium_models", "enterprise_models", "web_chat", "api_access", "conversation_sharing", "prompt_templates", "personas", "knowledge_bases", "priority_support", "advanced_analytics", "team_management", "workspace_sharing", "audit_log", "sso", "dedicated_support", "custom_models", "sla", "on_premise"]'::jsonb,
 30, 5);

-- ============================================================================
-- AI PROVIDERS
-- ============================================================================

INSERT INTO ai_gateway.providers (name, slug, base_url, api_version, auth_header, auth_prefix, api_key_enc, status, supported_features) VALUES
('OpenAI', 'openai', 'https://api.openai.com/v1', NULL, 'Authorization', 'Bearer', 'ENCRYPTED_KEY_PLACEHOLDER', 'active',
 ARRAY['streaming', 'function_calling', 'vision', 'json_mode', 'embeddings', 'image_generation', 'audio']),
('Anthropic', 'anthropic', 'https://api.anthropic.com/v1', '2024-01-01', 'x-api-key', '', 'ENCRYPTED_KEY_PLACEHOLDER', 'active',
 ARRAY['streaming', 'function_calling', 'vision']),
('Google AI', 'google', 'https://generativelanguage.googleapis.com/v1beta', NULL, 'x-goog-api-key', '', 'ENCRYPTED_KEY_PLACEHOLDER', 'active',
 ARRAY['streaming', 'function_calling', 'vision', 'embeddings']),
('Mistral AI', 'mistral', 'https://api.mistral.ai/v1', NULL, 'Authorization', 'Bearer', 'ENCRYPTED_KEY_PLACEHOLDER', 'active',
 ARRAY['streaming', 'function_calling', 'json_mode']),
('Meta (via Together)', 'meta', 'https://api.together.xyz/v1', NULL, 'Authorization', 'Bearer', 'ENCRYPTED_KEY_PLACEHOLDER', 'active',
 ARRAY['streaming']),
('DeepSeek', 'deepseek', 'https://api.deepseek.com/v1', NULL, 'Authorization', 'Bearer', 'ENCRYPTED_KEY_PLACEHOLDER', 'active',
 ARRAY['streaming', 'function_calling']);

-- ============================================================================
-- AI MODELS (representative selection)
-- ============================================================================

-- Get provider IDs for FK references
DO $$
DECLARE
    v_openai UUID;
    v_anthropic UUID;
    v_google UUID;
    v_mistral UUID;
    v_meta UUID;
    v_deepseek UUID;
BEGIN
    SELECT id INTO v_openai FROM ai_gateway.providers WHERE slug = 'openai';
    SELECT id INTO v_anthropic FROM ai_gateway.providers WHERE slug = 'anthropic';
    SELECT id INTO v_google FROM ai_gateway.providers WHERE slug = 'google';
    SELECT id INTO v_mistral FROM ai_gateway.providers WHERE slug = 'mistral';
    SELECT id INTO v_meta FROM ai_gateway.providers WHERE slug = 'meta';
    SELECT id INTO v_deepseek FROM ai_gateway.providers WHERE slug = 'deepseek';

    INSERT INTO ai_gateway.models (provider_id, name, slug, provider_model_id, category, max_context_tokens, max_output_tokens, supports_streaming, supports_function_calling, supports_vision, supports_json_mode, credit_multiplier, credit_per_input_token, credit_per_output_token, model_tier, is_active, is_visible, description, sort_order) VALUES
    -- OpenAI models
    (v_openai, 'GPT-4o', 'gpt-4o', 'gpt-4o', 'chat', 128000, 16384, TRUE, TRUE, TRUE, TRUE, 2.5, 0.00250000, 0.01000000, 'premium', TRUE, TRUE, 'Modelo mais capaz da OpenAI com visao e funcoes.', 1),
    (v_openai, 'GPT-4o Mini', 'gpt-4o-mini', 'gpt-4o-mini', 'chat', 128000, 16384, TRUE, TRUE, TRUE, TRUE, 1.0, 0.00015000, 0.00060000, 'basic', TRUE, TRUE, 'Modelo rapido e economico para tarefas simples.', 2),
    (v_openai, 'GPT-4 Turbo', 'gpt-4-turbo', 'gpt-4-turbo', 'chat', 128000, 4096, TRUE, TRUE, TRUE, TRUE, 3.0, 0.01000000, 0.03000000, 'premium', TRUE, TRUE, 'GPT-4 Turbo com visao e contexto de 128k.', 3),
    (v_openai, 'o1', 'o1', 'o1', 'chat', 200000, 100000, TRUE, TRUE, TRUE, FALSE, 4.0, 0.01500000, 0.06000000, 'enterprise', TRUE, TRUE, 'Modelo de raciocinio avancado da OpenAI.', 4),
    (v_openai, 'DALL-E 3', 'dall-e-3', 'dall-e-3', 'image_generation', 0, 0, FALSE, FALSE, FALSE, FALSE, 5.0, 0.00000000, 0.00000000, 'premium', TRUE, TRUE, 'Geracao de imagens de alta qualidade.', 10),
    (v_openai, 'text-embedding-3-small', 'embedding-3-small', 'text-embedding-3-small', 'embedding', 8191, 0, FALSE, FALSE, FALSE, FALSE, 0.2, 0.00002000, 0.00000000, 'basic', TRUE, FALSE, 'Embeddings para RAG e busca semantica.', 20),

    -- Anthropic models
    (v_anthropic, 'Claude 3.5 Sonnet', 'claude-3-5-sonnet', 'claude-3-5-sonnet-20241022', 'chat', 200000, 8192, TRUE, TRUE, TRUE, FALSE, 2.0, 0.00300000, 0.01500000, 'premium', TRUE, TRUE, 'Melhor equilibrio entre velocidade e inteligencia.', 5),
    (v_anthropic, 'Claude 3.5 Haiku', 'claude-3-5-haiku', 'claude-3-5-haiku-20241022', 'chat', 200000, 8192, TRUE, TRUE, FALSE, FALSE, 0.8, 0.00100000, 0.00500000, 'standard', TRUE, TRUE, 'Modelo rapido e acessivel da Anthropic.', 6),
    (v_anthropic, 'Claude 3 Opus', 'claude-3-opus', 'claude-3-opus-20240229', 'chat', 200000, 4096, TRUE, TRUE, TRUE, FALSE, 4.0, 0.01500000, 0.07500000, 'enterprise', TRUE, TRUE, 'Modelo mais poderoso da Anthropic.', 7),

    -- Google models
    (v_google, 'Gemini 2.0 Flash', 'gemini-2-flash', 'gemini-2.0-flash', 'chat', 1048576, 8192, TRUE, TRUE, TRUE, TRUE, 1.0, 0.00010000, 0.00040000, 'basic', TRUE, TRUE, 'Modelo ultra-rapido do Google com 1M de contexto.', 8),
    (v_google, 'Gemini 1.5 Pro', 'gemini-1-5-pro', 'gemini-1.5-pro', 'chat', 2097152, 8192, TRUE, TRUE, TRUE, TRUE, 2.5, 0.00125000, 0.00500000, 'premium', TRUE, TRUE, 'Modelo avancado com 2M tokens de contexto.', 9),

    -- Mistral models
    (v_mistral, 'Mistral Large', 'mistral-large', 'mistral-large-latest', 'chat', 128000, 4096, TRUE, TRUE, FALSE, TRUE, 2.0, 0.00200000, 0.00600000, 'standard', TRUE, TRUE, 'Modelo principal da Mistral.', 11),
    (v_mistral, 'Mistral Small', 'mistral-small', 'mistral-small-latest', 'chat', 128000, 4096, TRUE, TRUE, FALSE, TRUE, 0.5, 0.00020000, 0.00060000, 'basic', TRUE, TRUE, 'Modelo leve e eficiente.', 12),

    -- Meta models
    (v_meta, 'Llama 3.1 405B', 'llama-3-1-405b', 'meta-llama/Llama-3.1-405B-Instruct-Turbo', 'chat', 128000, 4096, TRUE, FALSE, FALSE, FALSE, 2.0, 0.00350000, 0.00350000, 'standard', TRUE, TRUE, 'Maior modelo open-source da Meta.', 13),
    (v_meta, 'Llama 3.1 70B', 'llama-3-1-70b', 'meta-llama/Llama-3.1-70B-Instruct-Turbo', 'chat', 128000, 4096, TRUE, FALSE, FALSE, FALSE, 0.8, 0.00088000, 0.00088000, 'basic', TRUE, TRUE, 'Modelo open-source equilibrado.', 14),

    -- DeepSeek models
    (v_deepseek, 'DeepSeek V3', 'deepseek-v3', 'deepseek-chat', 'chat', 64000, 8192, TRUE, TRUE, FALSE, TRUE, 0.5, 0.00014000, 0.00028000, 'basic', TRUE, TRUE, 'Modelo chines de alto desempenho e baixo custo.', 15),
    (v_deepseek, 'DeepSeek R1', 'deepseek-r1', 'deepseek-reasoner', 'chat', 64000, 8192, TRUE, FALSE, FALSE, FALSE, 1.5, 0.00055000, 0.00219000, 'standard', TRUE, TRUE, 'Modelo de raciocinio da DeepSeek.', 16);

    -- Set GPT-4o Mini as default model
    UPDATE ai_gateway.models SET is_default = TRUE WHERE slug = 'gpt-4o-mini';

END $$;

-- ============================================================================
-- DEFAULT RATE LIMITS (per plan tier)
-- ============================================================================

INSERT INTO ai_gateway.rate_limits (plan_tier, window, max_requests, max_tokens, description) VALUES
('free',       'minute', 5,    10000,    'Free tier: 5 RPM, 10k TPM'),
('free',       'day',    50,   100000,   'Free tier: 50 RPD, 100k TPD'),
('starter',    'minute', 20,   50000,    'Starter: 20 RPM, 50k TPM'),
('starter',    'day',    500,  1000000,  'Starter: 500 RPD, 1M TPD'),
('pro',        'minute', 60,   200000,   'Pro: 60 RPM, 200k TPM'),
('pro',        'day',    5000, 10000000, 'Pro: 5k RPD, 10M TPD'),
('team',       'minute', 60,   200000,   'Team: 60 RPM per member, 200k TPM'),
('team',       'day',    5000, 10000000, 'Team: 5k RPD per member, 10M TPD'),
('enterprise', 'minute', 200,  1000000,  'Enterprise: 200 RPM, 1M TPM'),
('enterprise', 'day',    50000,100000000,'Enterprise: 50k RPD, 100M TPD');

-- ============================================================================
-- DEFAULT FEATURE FLAGS
-- ============================================================================

INSERT INTO auth.feature_flags (name, description, is_enabled, rollout_percentage) VALUES
('new_chat_ui',        'Novo design da interface de chat',           FALSE, 0),
('rag_v2',             'RAG v2 com reranking e hybrid search',       FALSE, 0),
('prompt_marketplace', 'Marketplace de prompts e personas',          FALSE, 0),
('partner_dashboard',  'Dashboard de parceiros v2',                  FALSE, 0),
('streaming_v2',       'Streaming otimizado com SSE',                TRUE,  100),
('dark_mode',          'Tema escuro',                                TRUE,  100),
('multi_model_chat',   'Trocar modelo durante conversa',             TRUE,  100),
('conversation_fork',  'Fork de conversas',                          TRUE,  50),
('api_access',         'Acesso via API',                             TRUE,  100),
('knowledge_bases',    'Knowledge bases / RAG',                      TRUE,  100);
