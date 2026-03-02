-- ============================================================================
-- IA AGGREGATOR - Module 5: PARTNERS
-- File: 05_partners.sql
-- Purpose: Partners, coupons, attributions, commissions, payment batches,
--          anti-fraud scoring
-- Attribution: server-side click_id + cookie + manual + coupon
-- Payments: batch Pix payouts
-- ============================================================================

SET search_path TO partners, public;

-- ============================================================================
-- PARTNERS
-- ============================================================================

CREATE TABLE partners.partners (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) UNIQUE, -- 1:1 with user
    -- Identity
    display_name    VARCHAR(200) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    bio             TEXT,
    website_url     TEXT,
    logo_url        TEXT,
    -- Status
    status          partner_status NOT NULL DEFAULT 'pending',
    tier            partner_tier NOT NULL DEFAULT 'bronze',
    approved_at     TIMESTAMPTZ,
    approved_by     UUID REFERENCES auth.users(id),
    -- Commission rates (percentage, can be overridden per coupon)
    default_commission_rate NUMERIC(5,2) NOT NULL DEFAULT 20.00, -- 20% default
    recurring_commission_rate NUMERIC(5,2) DEFAULT 10.00, -- for recurring subs
    recurring_commission_months INTEGER DEFAULT 12, -- how many months of recurring
    -- Payment details (PIX)
    pix_key_type    VARCHAR(20), -- 'cpf', 'cnpj', 'email', 'phone', 'random'
    pix_key_enc     TEXT, -- encrypted PIX key
    bank_name       VARCHAR(100),
    -- Legal
    cpf_cnpj_hash   TEXT, -- hashed for verification
    contract_signed_at TIMESTAMPTZ,
    contract_version VARCHAR(20),
    -- Performance metrics (denormalized for quick access)
    total_referrals INTEGER NOT NULL DEFAULT 0,
    total_conversions INTEGER NOT NULL DEFAULT 0,
    total_revenue_cents BIGINT NOT NULL DEFAULT 0,
    total_commissions_cents BIGINT NOT NULL DEFAULT 0,
    total_paid_cents BIGINT NOT NULL DEFAULT 0,
    conversion_rate NUMERIC(5,2) DEFAULT 0.0,
    -- Anti-fraud
    fraud_score     NUMERIC(5,2) DEFAULT 0.0, -- 0-100, higher = more suspicious
    fraud_flags     TEXT[] DEFAULT ARRAY[]::TEXT[],
    is_fraud_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_reviewed_at TIMESTAMPTZ,
    fraud_reviewed_by UUID REFERENCES auth.users(id),
    -- Metadata
    settings        JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    suspended_at    TIMESTAMPTZ,
    terminated_at   TIMESTAMPTZ,
    suspension_reason TEXT,
    termination_reason TEXT
);

COMMENT ON TABLE partners.partners IS 'Partner/affiliate accounts. Each partner is linked 1:1 with a user account.';
COMMENT ON COLUMN partners.partners.default_commission_rate IS 'Default commission percentage for new sales attributed to this partner.';
COMMENT ON COLUMN partners.partners.fraud_score IS 'ML-computed fraud score 0-100. Higher values indicate higher fraud risk.';

CREATE INDEX idx_partners_user ON partners.partners(user_id);
CREATE INDEX idx_partners_slug ON partners.partners(slug);
CREATE INDEX idx_partners_status ON partners.partners(status);
CREATE INDEX idx_partners_tier ON partners.partners(tier);
CREATE INDEX idx_partners_fraud ON partners.partners(fraud_score DESC);

CREATE TRIGGER trg_partners_updated_at
    BEFORE UPDATE ON partners.partners
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- COUPONS
-- ============================================================================

CREATE TABLE partners.coupons (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    partner_id      UUID REFERENCES partners.partners(id), -- NULL = platform coupon
    -- Coupon identity
    code            VARCHAR(50) NOT NULL UNIQUE,
    display_name    VARCHAR(200),
    description     TEXT,
    -- Type and value
    coupon_type     coupon_type NOT NULL,
    value           NUMERIC(10,2) NOT NULL, -- percentage, fixed amount, or credits
    -- Constraints
    min_plan_tier   plan_tier, -- minimum plan required
    applicable_plans UUID[], -- specific plan IDs, NULL = all plans
    applicable_billing_cycles billing_cycle[], -- monthly, yearly, or both
    -- Usage limits
    max_redemptions INTEGER, -- total uses allowed
    max_per_user    INTEGER DEFAULT 1,
    current_redemptions INTEGER NOT NULL DEFAULT 0,
    -- Duration
    duration_months INTEGER, -- how many months discount applies (NULL = forever)
    -- Validity
    starts_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    -- Commission override for this coupon
    commission_rate_override NUMERIC(5,2), -- overrides partner default
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- First purchase only?
    first_purchase_only BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE partners.coupons IS 'Discount coupons. Can be partner-linked (affiliate) or platform-wide promotions.';
COMMENT ON COLUMN partners.coupons.value IS 'Numeric value: percentage (e.g., 20.00 for 20%), fixed BRL amount, or number of bonus credits.';
COMMENT ON COLUMN partners.coupons.duration_months IS 'How many billing cycles the discount applies. NULL means permanent.';

CREATE INDEX idx_coupons_code ON partners.coupons(code);
CREATE INDEX idx_coupons_partner ON partners.coupons(partner_id);
CREATE INDEX idx_coupons_active ON partners.coupons(is_active, starts_at, expires_at);

CREATE TRIGGER trg_coupons_updated_at
    BEFORE UPDATE ON partners.coupons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Now add FK from billing.subscriptions to coupons
ALTER TABLE billing.subscriptions
    ADD CONSTRAINT fk_subscriptions_coupon
    FOREIGN KEY (coupon_id) REFERENCES partners.coupons(id);

-- ============================================================================
-- COUPON REDEMPTIONS
-- ============================================================================

CREATE TABLE partners.coupon_redemptions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    coupon_id       UUID NOT NULL REFERENCES partners.coupons(id),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    subscription_id UUID REFERENCES billing.subscriptions(id),
    -- Redemption details
    discount_applied_cents BIGINT, -- actual discount in BRL centavos
    credits_awarded BIGINT, -- if coupon gives credits
    -- Metadata
    ip_address      INET,
    user_agent      TEXT,
    redeemed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_coupon_redemption_user UNIQUE (coupon_id, user_id)
);

COMMENT ON TABLE partners.coupon_redemptions IS 'Record of each coupon use. Enforces max_per_user constraint.';

CREATE INDEX idx_redemptions_coupon ON partners.coupon_redemptions(coupon_id);
CREATE INDEX idx_redemptions_user ON partners.coupon_redemptions(user_id);
CREATE INDEX idx_redemptions_date ON partners.coupon_redemptions(redeemed_at);

-- ============================================================================
-- REFERRAL LINKS
-- ============================================================================

CREATE TABLE partners.referral_links (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    partner_id      UUID NOT NULL REFERENCES partners.partners(id) ON DELETE CASCADE,
    -- Link details
    slug            VARCHAR(100) NOT NULL UNIQUE, -- e.g., "joao-dev"
    destination_url TEXT NOT NULL DEFAULT '/', -- where the link points to
    -- UTM parameters
    utm_source      VARCHAR(100),
    utm_medium      VARCHAR(100) DEFAULT 'referral',
    utm_campaign    VARCHAR(100),
    -- Associated coupon (optional auto-apply)
    coupon_id       UUID REFERENCES partners.coupons(id),
    -- Statistics (denormalized)
    click_count     BIGINT NOT NULL DEFAULT 0,
    unique_click_count BIGINT NOT NULL DEFAULT 0,
    conversion_count INTEGER NOT NULL DEFAULT 0,
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE partners.referral_links IS 'Partner referral/tracking links. Each click generates a click_id for attribution.';

CREATE INDEX idx_referral_links_partner ON partners.referral_links(partner_id);
CREATE INDEX idx_referral_links_slug ON partners.referral_links(slug);
CREATE INDEX idx_referral_links_coupon ON partners.referral_links(coupon_id);

CREATE TRIGGER trg_referral_links_updated_at
    BEFORE UPDATE ON partners.referral_links
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- CLICK TRACKING
-- ============================================================================

CREATE TABLE partners.clicks (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    referral_link_id UUID NOT NULL REFERENCES partners.referral_links(id),
    partner_id      UUID NOT NULL REFERENCES partners.partners(id),
    -- Click ID (server-generated, stored in cookie)
    click_id        VARCHAR(64) NOT NULL UNIQUE,
    -- Visitor data
    ip_address      INET,
    user_agent      TEXT,
    referer_url     TEXT,
    -- Device info (parsed from user_agent)
    device_type     VARCHAR(20), -- desktop, mobile, tablet
    browser         VARCHAR(50),
    os              VARCHAR(50),
    country         VARCHAR(2), -- ISO country code
    region          VARCHAR(100),
    city            VARCHAR(100),
    -- UTM passed through
    utm_source      VARCHAR(100),
    utm_medium      VARCHAR(100),
    utm_campaign    VARCHAR(100),
    utm_content     VARCHAR(100),
    utm_term        VARCHAR(100),
    -- Conversion tracking
    converted       BOOLEAN NOT NULL DEFAULT FALSE,
    converted_user_id UUID REFERENCES auth.users(id),
    converted_at    TIMESTAMPTZ,
    -- Cookie / fingerprint
    fingerprint_hash TEXT, -- browser fingerprint hash for dedup
    cookie_expires_at TIMESTAMPTZ, -- when the attribution cookie expires (30-90 days)
    -- Anti-fraud
    is_suspicious   BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_signals   JSONB DEFAULT '{}'::jsonb,
    -- Metadata
    clicked_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE partners.clicks IS 'Server-side click tracking for attribution. Each click gets a unique click_id stored in user cookie.';
COMMENT ON COLUMN partners.clicks.click_id IS 'Server-generated unique ID stored in cookie for attribution window (30-90 days).';
COMMENT ON COLUMN partners.clicks.fraud_signals IS 'JSON flags: bot_detected, vpn, datacenter_ip, rapid_clicks, etc.';

CREATE INDEX idx_clicks_referral_link ON partners.clicks(referral_link_id);
CREATE INDEX idx_clicks_partner ON partners.clicks(partner_id);
CREATE INDEX idx_clicks_click_id ON partners.clicks(click_id);
CREATE INDEX idx_clicks_ip ON partners.clicks(ip_address);
CREATE INDEX idx_clicks_converted ON partners.clicks(converted) WHERE converted = TRUE;
CREATE INDEX idx_clicks_date ON partners.clicks(clicked_at);
CREATE INDEX idx_clicks_fingerprint ON partners.clicks(fingerprint_hash) WHERE fingerprint_hash IS NOT NULL;

-- ============================================================================
-- ATTRIBUTIONS (connects a conversion to a partner)
-- ============================================================================

CREATE TABLE partners.attributions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Who converted
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- Attributed to
    partner_id      UUID NOT NULL REFERENCES partners.partners(id),
    -- How was it attributed
    source          attribution_source NOT NULL,
    click_id        UUID REFERENCES partners.clicks(id),
    coupon_id       UUID REFERENCES partners.coupons(id),
    referral_link_id UUID REFERENCES partners.referral_links(id),
    -- Manual attribution (admin override)
    manual_reason   TEXT,
    manual_by       UUID REFERENCES auth.users(id),
    -- What was purchased
    subscription_id UUID REFERENCES billing.subscriptions(id),
    plan_tier       plan_tier,
    -- Attribution window
    click_to_conversion_hours NUMERIC(8,2), -- time from click to signup/purchase
    -- Anti-fraud
    fraud_risk      fraud_risk_level NOT NULL DEFAULT 'low',
    fraud_score     NUMERIC(5,2) DEFAULT 0.0,
    fraud_signals   JSONB DEFAULT '{}'::jsonb,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at     TIMESTAMPTZ,
    verified_by     UUID REFERENCES auth.users(id),
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    revoked_at      TIMESTAMPTZ,
    revoke_reason   TEXT,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE partners.attributions IS 'Links a user conversion/subscription to a partner. Supports cookie, click_id, coupon, and manual attribution.';
COMMENT ON COLUMN partners.attributions.fraud_risk IS 'ML-assessed fraud risk level for this specific attribution.';
COMMENT ON COLUMN partners.attributions.click_to_conversion_hours IS 'Hours between first click and conversion for attribution window validation.';

CREATE INDEX idx_attributions_user ON partners.attributions(user_id);
CREATE INDEX idx_attributions_partner ON partners.attributions(partner_id);
CREATE INDEX idx_attributions_source ON partners.attributions(source);
CREATE INDEX idx_attributions_fraud ON partners.attributions(fraud_risk);
CREATE INDEX idx_attributions_created ON partners.attributions(created_at);
CREATE INDEX idx_attributions_active ON partners.attributions(is_active) WHERE is_active = TRUE;

-- ============================================================================
-- COMMISSIONS
-- ============================================================================

CREATE TABLE partners.commissions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    partner_id      UUID NOT NULL REFERENCES partners.partners(id),
    attribution_id  UUID NOT NULL REFERENCES partners.attributions(id),
    -- What triggered the commission
    payment_id      UUID REFERENCES billing.payments(id),
    subscription_id UUID REFERENCES billing.subscriptions(id),
    -- Commission details
    status          commission_status NOT NULL DEFAULT 'pending',
    -- Amounts (BRL centavos)
    base_amount_cents BIGINT NOT NULL, -- original payment amount
    commission_rate NUMERIC(5,2) NOT NULL, -- percentage applied
    commission_amount_cents BIGINT NOT NULL, -- calculated commission
    -- Is this a recurring commission?
    is_recurring    BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_month INTEGER, -- which month of recurring (1, 2, 3...)
    -- Payment batch reference
    payment_batch_id UUID, -- FK added below
    -- Anti-fraud
    fraud_hold      BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_hold_reason TEXT,
    fraud_cleared_at TIMESTAMPTZ,
    fraud_cleared_by UUID REFERENCES auth.users(id),
    -- Lifecycle
    approved_at     TIMESTAMPTZ,
    approved_by     UUID REFERENCES auth.users(id),
    paid_at         TIMESTAMPTZ,
    rejected_at     TIMESTAMPTZ,
    rejected_by     UUID REFERENCES auth.users(id),
    reject_reason   TEXT,
    clawed_back_at  TIMESTAMPTZ,
    clawback_reason TEXT,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE partners.commissions IS 'Individual commission records. Created per payment, tracks approval flow and fraud holds.';
COMMENT ON COLUMN partners.commissions.is_recurring IS 'True if this commission is from a recurring subscription renewal (not first purchase).';
COMMENT ON COLUMN partners.commissions.clawed_back_at IS 'If set, commission was reversed (e.g., due to refund or fraud).';

CREATE INDEX idx_commissions_partner ON partners.commissions(partner_id);
CREATE INDEX idx_commissions_attribution ON partners.commissions(attribution_id);
CREATE INDEX idx_commissions_status ON partners.commissions(status);
CREATE INDEX idx_commissions_batch ON partners.commissions(payment_batch_id);
CREATE INDEX idx_commissions_fraud ON partners.commissions(fraud_hold) WHERE fraud_hold = TRUE;
CREATE INDEX idx_commissions_created ON partners.commissions(created_at);

CREATE TRIGGER trg_commissions_updated_at
    BEFORE UPDATE ON partners.commissions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- PAYMENT BATCHES (batch Pix payouts to partners)
-- ============================================================================

CREATE TABLE partners.payment_batches (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Batch details
    batch_number    VARCHAR(50) NOT NULL UNIQUE,
    description     TEXT,
    -- Totals
    total_amount_cents BIGINT NOT NULL DEFAULT 0,
    total_commissions INTEGER NOT NULL DEFAULT 0,
    total_partners  INTEGER NOT NULL DEFAULT 0,
    -- Status
    status          payout_status NOT NULL DEFAULT 'pending',
    -- Processing
    initiated_by    UUID NOT NULL REFERENCES auth.users(id),
    approved_by     UUID REFERENCES auth.users(id),
    approved_at     TIMESTAMPTZ,
    processing_started_at TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    failed_at       TIMESTAMPTZ,
    failure_reason  TEXT,
    -- Payment gateway
    gateway_batch_id VARCHAR(255), -- external batch ID from payment processor
    gateway_response JSONB,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE partners.payment_batches IS 'Batch Pix payout processing. Groups approved commissions for bulk payment.';

CREATE INDEX idx_payment_batches_status ON partners.payment_batches(status);
CREATE INDEX idx_payment_batches_created ON partners.payment_batches(created_at);

CREATE TRIGGER trg_payment_batches_updated_at
    BEFORE UPDATE ON partners.payment_batches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add FK from commissions to payment_batches
ALTER TABLE partners.commissions
    ADD CONSTRAINT fk_commissions_batch
    FOREIGN KEY (payment_batch_id) REFERENCES partners.payment_batches(id);

-- ============================================================================
-- PAYMENT BATCH ITEMS (individual payouts within a batch)
-- ============================================================================

CREATE TABLE partners.payment_batch_items (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    batch_id        UUID NOT NULL REFERENCES partners.payment_batches(id) ON DELETE CASCADE,
    partner_id      UUID NOT NULL REFERENCES partners.partners(id),
    -- Payout details
    amount_cents    BIGINT NOT NULL,
    commission_ids  UUID[] NOT NULL, -- array of commission IDs in this payout
    -- PIX payment details
    pix_key_type    VARCHAR(20) NOT NULL,
    pix_key_enc     TEXT NOT NULL, -- encrypted, snapshot at time of batch
    -- Status
    status          payout_status NOT NULL DEFAULT 'pending',
    -- Transaction result
    pix_transaction_id VARCHAR(255),
    pix_end_to_end_id VARCHAR(255), -- PIX E2E ID from BCB
    processed_at    TIMESTAMPTZ,
    error_message   TEXT,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE partners.payment_batch_items IS 'Individual partner payouts within a batch. One item per partner per batch.';
COMMENT ON COLUMN partners.payment_batch_items.pix_end_to_end_id IS 'Central Bank of Brazil end-to-end PIX transaction ID.';

CREATE INDEX idx_batch_items_batch ON partners.payment_batch_items(batch_id);
CREATE INDEX idx_batch_items_partner ON partners.payment_batch_items(partner_id);
CREATE INDEX idx_batch_items_status ON partners.payment_batch_items(status);

CREATE TRIGGER trg_batch_items_updated_at
    BEFORE UPDATE ON partners.payment_batch_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- FRAUD EVENTS (anti-fraud log)
-- ============================================================================

CREATE TABLE partners.fraud_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- What entity triggered the event
    partner_id      UUID REFERENCES partners.partners(id),
    attribution_id  UUID REFERENCES partners.attributions(id),
    commission_id   UUID REFERENCES partners.commissions(id),
    click_id        UUID REFERENCES partners.clicks(id),
    -- Event details
    event_type      VARCHAR(100) NOT NULL, -- 'suspicious_click_pattern', 'self_referral', 'vpn_detected', etc.
    risk_level      fraud_risk_level NOT NULL,
    score           NUMERIC(5,2) NOT NULL,
    -- Details
    description     TEXT,
    signals         JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- Resolution
    resolved        BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at     TIMESTAMPTZ,
    resolved_by     UUID REFERENCES auth.users(id),
    resolution      TEXT, -- 'confirmed_fraud', 'false_positive', 'needs_review'
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE partners.fraud_events IS 'Anti-fraud event log. ML-generated alerts for suspicious partner activity.';
COMMENT ON COLUMN partners.fraud_events.signals IS 'JSON details: {ip_mismatch, rapid_clicks, same_device, cookie_manipulation, etc.}';

CREATE INDEX idx_fraud_events_partner ON partners.fraud_events(partner_id);
CREATE INDEX idx_fraud_events_risk ON partners.fraud_events(risk_level);
CREATE INDEX idx_fraud_events_resolved ON partners.fraud_events(resolved) WHERE resolved = FALSE;
CREATE INDEX idx_fraud_events_created ON partners.fraud_events(created_at);
