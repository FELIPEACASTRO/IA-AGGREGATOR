-- ============================================================================
-- V1: Extensions, Enum Types, Utility Functions, and Schemas
-- ============================================================================

-- Required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "vector";

-- ============================================================================
-- SCHEMAS
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS billing;
CREATE SCHEMA IF NOT EXISTS chat;
CREATE SCHEMA IF NOT EXISTS ai_gateway;
CREATE SCHEMA IF NOT EXISTS partners;
CREATE SCHEMA IF NOT EXISTS content;
CREATE SCHEMA IF NOT EXISTS teams;
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE SCHEMA IF NOT EXISTS audit;

-- ============================================================================
-- SHARED ENUM TYPES
-- ============================================================================

-- Auth
CREATE TYPE user_status AS ENUM ('active', 'inactive', 'suspended', 'pending_verification', 'deleted');
CREATE TYPE auth_provider AS ENUM ('email', 'google', 'github', 'microsoft', 'apple');
CREATE TYPE user_role AS ENUM ('super_admin', 'admin', 'user', 'viewer', 'api_only');
CREATE TYPE org_type AS ENUM ('personal', 'team', 'enterprise');
CREATE TYPE session_status AS ENUM ('active', 'expired', 'revoked');
CREATE TYPE consent_type AS ENUM ('terms_of_service', 'privacy_policy', 'marketing', 'data_processing', 'cookie_policy');
CREATE TYPE erasure_status AS ENUM ('requested', 'processing', 'completed', 'failed', 'denied');

-- Billing
CREATE TYPE plan_tier AS ENUM ('free', 'starter', 'pro', 'team', 'enterprise');
CREATE TYPE billing_cycle AS ENUM ('monthly', 'yearly');
CREATE TYPE subscription_status AS ENUM ('active', 'trialing', 'past_due', 'canceled', 'paused', 'expired');
CREATE TYPE payment_status AS ENUM ('pending', 'processing', 'succeeded', 'failed', 'refunded', 'disputed', 'canceled');
CREATE TYPE payment_method_type AS ENUM ('credit_card', 'pix', 'boleto', 'bank_transfer');
CREATE TYPE invoice_status AS ENUM ('draft', 'open', 'paid', 'void', 'uncollectible');
CREATE TYPE credit_tx_type AS ENUM ('purchase', 'usage', 'refund', 'bonus', 'rollover', 'adjustment', 'partner_reward', 'expiration');

-- Chat
CREATE TYPE conversation_status AS ENUM ('active', 'archived', 'deleted');
CREATE TYPE message_role AS ENUM ('system', 'user', 'assistant', 'tool');
CREATE TYPE message_status AS ENUM ('pending', 'streaming', 'completed', 'failed', 'canceled');
CREATE TYPE share_visibility AS ENUM ('private', 'unlisted', 'public');
CREATE TYPE attachment_type AS ENUM ('image', 'document', 'audio', 'video', 'code', 'other');

-- AI Gateway
CREATE TYPE provider_status AS ENUM ('active', 'degraded', 'maintenance', 'disabled');
CREATE TYPE model_category AS ENUM ('chat', 'completion', 'embedding', 'image_generation', 'audio_transcription', 'audio_tts', 'vision', 'code');
CREATE TYPE routing_strategy AS ENUM ('lowest_cost', 'lowest_latency', 'highest_quality', 'round_robin', 'weighted', 'fallback');
CREATE TYPE cache_status AS ENUM ('hit', 'miss', 'expired', 'bypassed');
CREATE TYPE rate_limit_window AS ENUM ('second', 'minute', 'hour', 'day');

-- Partners
CREATE TYPE partner_status AS ENUM ('pending', 'approved', 'active', 'suspended', 'terminated');
CREATE TYPE partner_tier AS ENUM ('bronze', 'silver', 'gold', 'platinum');
CREATE TYPE coupon_type AS ENUM ('percentage', 'fixed_amount', 'credits', 'trial_extension');
CREATE TYPE attribution_source AS ENUM ('cookie', 'click_id', 'manual', 'coupon', 'referral_link');
CREATE TYPE commission_status AS ENUM ('pending', 'approved', 'paid', 'rejected', 'clawed_back');
CREATE TYPE payout_status AS ENUM ('pending', 'processing', 'completed', 'failed', 'canceled');
CREATE TYPE fraud_risk_level AS ENUM ('low', 'medium', 'high', 'critical');

-- Content
CREATE TYPE persona_visibility AS ENUM ('private', 'workspace', 'public', 'marketplace');
CREATE TYPE prompt_type AS ENUM ('system', 'user', 'few_shot', 'chain_of_thought');
CREATE TYPE template_category AS ENUM ('writing', 'coding', 'analysis', 'translation', 'summarization', 'creative', 'business', 'education', 'custom');
CREATE TYPE kb_source_type AS ENUM ('file_upload', 'url_crawl', 'api_sync', 'manual_entry', 'notion', 'google_drive', 'confluence');
CREATE TYPE kb_processing_status AS ENUM ('pending', 'processing', 'indexed', 'failed', 'reindexing');
CREATE TYPE chunk_status AS ENUM ('pending', 'embedded', 'failed');

-- Teams
CREATE TYPE workspace_role AS ENUM ('owner', 'admin', 'member', 'viewer', 'billing_admin');
CREATE TYPE invite_status AS ENUM ('pending', 'accepted', 'declined', 'expired', 'revoked');

-- Analytics
CREATE TYPE event_category AS ENUM ('auth', 'chat', 'billing', 'ai_request', 'partner', 'content', 'team', 'system');
CREATE TYPE report_type AS ENUM ('usage', 'billing', 'performance', 'partner', 'audit', 'custom');
CREATE TYPE aggregation_period AS ENUM ('hourly', 'daily', 'weekly', 'monthly');

-- ============================================================================
-- UTILITY FUNCTIONS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_short_id(length INTEGER DEFAULT 10)
RETURNS TEXT AS $$
DECLARE
    chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    result TEXT := '';
    i INTEGER;
BEGIN
    FOR i IN 1..length LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::int, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- RLS context functions
CREATE OR REPLACE FUNCTION set_current_user_id(p_user_id UUID)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_user_id', p_user_id::text, false);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_current_user_id()
RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_user_id', true), '')::UUID;
EXCEPTION WHEN OTHERS THEN RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION set_current_org_id(p_org_id UUID)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_org_id', p_org_id::text, false);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_current_org_id()
RETURNS UUID AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_org_id', true), '')::UUID;
EXCEPTION WHEN OTHERS THEN RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

CREATE OR REPLACE FUNCTION is_super_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM auth.users
        WHERE id = get_current_user_id()
          AND role = 'super_admin'
          AND status = 'active'
    );
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;
