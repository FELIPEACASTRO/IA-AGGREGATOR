-- ============================================================================
-- V3: Add auth_provider and provider_user_id columns to users table
-- These columns are needed for quick auth-type checks without joining
-- user_identities. The canonical provider list remains in user_identities.
-- ============================================================================

SET search_path TO auth, public;

ALTER TABLE auth.users
    ADD COLUMN IF NOT EXISTS auth_provider auth_provider NOT NULL DEFAULT 'email',
    ADD COLUMN IF NOT EXISTS provider_user_id VARCHAR(255);

CREATE INDEX idx_users_auth_provider ON auth.users(auth_provider);

COMMENT ON COLUMN auth.users.auth_provider IS 'Primary authentication provider for this user (denormalized from user_identities for quick checks)';
COMMENT ON COLUMN auth.users.provider_user_id IS 'Provider-specific user ID for the primary auth provider';
