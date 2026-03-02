-- ============================================================================
-- IA AGGREGATOR - Module 2: BILLING
-- File: 02_billing.sql
-- Purpose: Plans, subscriptions, credits, payments, invoices
-- Pricing: Free(R$0/300cr), Starter(R$39/1000cr), Pro(R$99/4000cr),
--          Team(R$49/seat), Enterprise(custom)
-- ============================================================================

SET search_path TO billing, public;

-- ============================================================================
-- PLANS
-- ============================================================================

CREATE TABLE billing.plans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    -- Identity
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(50) NOT NULL UNIQUE,
    tier            plan_tier NOT NULL,
    description     TEXT,
    -- Pricing (amounts in BRL centavos)
    price_monthly_cents BIGINT NOT NULL DEFAULT 0,
    price_yearly_cents  BIGINT NOT NULL DEFAULT 0,
    -- Per-seat pricing (Team/Enterprise)
    is_per_seat     BOOLEAN NOT NULL DEFAULT FALSE,
    min_seats       INTEGER DEFAULT 1,
    max_seats       INTEGER,
    -- Credits
    monthly_credits BIGINT NOT NULL DEFAULT 0,
    credit_rollover_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    credit_rollover_max BIGINT, -- max credits that can roll over
    credit_rollover_months INTEGER DEFAULT 1, -- how many months credits survive
    -- Limits
    max_conversations_per_day INTEGER,
    max_messages_per_conversation INTEGER,
    max_tokens_per_request INTEGER,
    max_file_upload_mb INTEGER DEFAULT 10,
    max_knowledge_bases INTEGER DEFAULT 1,
    max_api_keys INTEGER DEFAULT 3,
    -- Features (JSON array of feature slugs)
    features        JSONB NOT NULL DEFAULT '[]'::jsonb,
    -- Models access (which model tiers are available)
    allowed_model_tiers TEXT[] NOT NULL DEFAULT ARRAY['basic'],
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_visible      BOOLEAN NOT NULL DEFAULT TRUE, -- show on pricing page
    sort_order      INTEGER NOT NULL DEFAULT 0,
    -- Trial
    trial_days      INTEGER DEFAULT 0,
    -- Metadata
    stripe_price_id_monthly VARCHAR(255),
    stripe_price_id_yearly  VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE billing.plans IS 'Available subscription plans. 5 tiers: free, starter, pro, team, enterprise.';
COMMENT ON COLUMN billing.plans.price_monthly_cents IS 'Monthly price in BRL centavos (e.g., 3900 = R$39.00).';
COMMENT ON COLUMN billing.plans.monthly_credits IS 'Number of credits included per month. Free=300, Starter=1000, Pro=4000.';
COMMENT ON COLUMN billing.plans.features IS 'JSON array of enabled feature slugs: ["gpt4", "dalle", "api_access", "priority_support", ...].';
COMMENT ON COLUMN billing.plans.allowed_model_tiers IS 'Which model tiers this plan can access: basic, standard, premium, enterprise.';

CREATE INDEX idx_plans_tier ON billing.plans(tier);
CREATE INDEX idx_plans_active ON billing.plans(is_active, is_visible);

CREATE TRIGGER trg_plans_updated_at
    BEFORE UPDATE ON billing.plans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- PLAN FEATURES (detailed feature matrix)
-- ============================================================================

CREATE TABLE billing.plan_features (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id         UUID NOT NULL REFERENCES billing.plans(id) ON DELETE CASCADE,
    feature_key     VARCHAR(100) NOT NULL, -- e.g., 'max_file_size_mb', 'rag_enabled'
    feature_value   TEXT NOT NULL, -- string representation of value
    feature_type    VARCHAR(20) NOT NULL DEFAULT 'boolean', -- boolean, integer, string
    display_name    VARCHAR(200),
    display_order   INTEGER DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_plan_feature UNIQUE (plan_id, feature_key)
);

COMMENT ON TABLE billing.plan_features IS 'Detailed feature matrix for plan comparison. Each row is one feature toggle/limit.';

CREATE INDEX idx_plan_features_plan ON billing.plan_features(plan_id);

-- ============================================================================
-- SUBSCRIPTIONS
-- ============================================================================

CREATE TABLE billing.subscriptions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    plan_id         UUID NOT NULL REFERENCES billing.plans(id),
    -- Status
    status          subscription_status NOT NULL DEFAULT 'active',
    billing_cycle   billing_cycle NOT NULL DEFAULT 'monthly',
    -- Dates
    current_period_start TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    current_period_end   TIMESTAMPTZ NOT NULL,
    trial_start     TIMESTAMPTZ,
    trial_end       TIMESTAMPTZ,
    canceled_at     TIMESTAMPTZ,
    cancel_reason   TEXT,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    -- Seats (for Team plan)
    seat_count      INTEGER NOT NULL DEFAULT 1,
    -- External
    stripe_subscription_id VARCHAR(255) UNIQUE,
    stripe_customer_id     VARCHAR(255),
    -- Coupon/discount
    coupon_id       UUID, -- FK added after partners module
    discount_percent SMALLINT DEFAULT 0 CHECK (discount_percent BETWEEN 0 AND 100),
    discount_expires_at TIMESTAMPTZ,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE billing.subscriptions IS 'Active and historical subscriptions. One active subscription per org.';
COMMENT ON COLUMN billing.subscriptions.cancel_at_period_end IS 'If true, subscription remains active until current_period_end, then expires.';

CREATE INDEX idx_subscriptions_user ON billing.subscriptions(user_id);
CREATE INDEX idx_subscriptions_org ON billing.subscriptions(org_id);
CREATE INDEX idx_subscriptions_status ON billing.subscriptions(status);
CREATE INDEX idx_subscriptions_stripe ON billing.subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscriptions_period_end ON billing.subscriptions(current_period_end);

CREATE TRIGGER trg_subscriptions_updated_at
    BEFORE UPDATE ON billing.subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- CREDIT BALANCES (current balance per org)
-- ============================================================================

CREATE TABLE billing.credit_balances (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) UNIQUE,
    -- Current balances
    balance         BIGINT NOT NULL DEFAULT 0, -- current available credits
    lifetime_earned BIGINT NOT NULL DEFAULT 0, -- total credits ever received
    lifetime_spent  BIGINT NOT NULL DEFAULT 0, -- total credits ever consumed
    -- Period tracking
    period_credits_used BIGINT NOT NULL DEFAULT 0, -- credits used in current billing period
    period_reset_at TIMESTAMPTZ, -- when the period counter resets
    -- Rollover
    rollover_credits BIGINT NOT NULL DEFAULT 0, -- credits carried from previous period
    rollover_expires_at TIMESTAMPTZ,
    -- Alerts
    low_balance_alerted BOOLEAN NOT NULL DEFAULT FALSE,
    -- Metadata
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE billing.credit_balances IS 'Real-time credit balance per organization. Updated via credit_transactions.';

CREATE INDEX idx_credit_balances_org ON billing.credit_balances(org_id);

CREATE TRIGGER trg_credit_balances_updated_at
    BEFORE UPDATE ON billing.credit_balances
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- CREDIT TRANSACTIONS (immutable ledger)
-- ============================================================================

CREATE TABLE billing.credit_transactions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    user_id         UUID REFERENCES auth.users(id), -- user who triggered
    -- Transaction details
    tx_type         credit_tx_type NOT NULL,
    amount          BIGINT NOT NULL, -- positive = credit, negative = debit
    balance_after   BIGINT NOT NULL, -- balance after this transaction
    -- Reference to what caused this transaction
    reference_type  VARCHAR(50), -- 'ai_request', 'subscription_renewal', 'purchase', 'refund', etc.
    reference_id    UUID, -- ID of the related entity
    -- AI usage details (when tx_type = 'usage')
    model_id        UUID, -- FK to ai_gateway.models
    tokens_input    INTEGER,
    tokens_output   INTEGER,
    credit_multiplier NUMERIC(6,4) DEFAULT 1.0, -- per-model multiplier
    -- Description
    description     TEXT,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT credit_tx_nonzero CHECK (amount != 0)
);

COMMENT ON TABLE billing.credit_transactions IS 'Immutable ledger of all credit movements. Never updated or deleted.';
COMMENT ON COLUMN billing.credit_transactions.amount IS 'Positive for credits added (purchase, bonus), negative for credits consumed (usage).';
COMMENT ON COLUMN billing.credit_transactions.credit_multiplier IS 'Per-model cost multiplier. GPT-4 might be 3.0x, GPT-3.5 might be 1.0x.';

CREATE INDEX idx_credit_tx_org ON billing.credit_transactions(org_id);
CREATE INDEX idx_credit_tx_user ON billing.credit_transactions(user_id);
CREATE INDEX idx_credit_tx_type ON billing.credit_transactions(tx_type);
CREATE INDEX idx_credit_tx_created ON billing.credit_transactions(created_at);
CREATE INDEX idx_credit_tx_reference ON billing.credit_transactions(reference_type, reference_id);
-- Partition-ready index for time-series queries
CREATE INDEX idx_credit_tx_org_created ON billing.credit_transactions(org_id, created_at DESC);

-- ============================================================================
-- CREDIT PACKAGES (purchasable top-ups)
-- ============================================================================

CREATE TABLE billing.credit_packages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(50) NOT NULL UNIQUE,
    credits         BIGINT NOT NULL,
    price_cents     BIGINT NOT NULL, -- BRL centavos
    bonus_credits   BIGINT NOT NULL DEFAULT 0, -- extra credits as bonus
    -- Availability
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    available_for_tiers plan_tier[] DEFAULT ARRAY['starter', 'pro', 'team', 'enterprise'],
    -- Display
    badge_text      VARCHAR(50), -- e.g., "Best Value", "Popular"
    sort_order      INTEGER DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE billing.credit_packages IS 'Purchasable credit top-up packages beyond monthly allocation.';

CREATE TRIGGER trg_credit_packages_updated_at
    BEFORE UPDATE ON billing.credit_packages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- PAYMENTS
-- ============================================================================

CREATE TABLE billing.payments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    subscription_id UUID REFERENCES billing.subscriptions(id),
    -- Amount (BRL centavos)
    amount_cents    BIGINT NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'BRL',
    -- Payment method
    payment_method  payment_method_type NOT NULL,
    -- Status
    status          payment_status NOT NULL DEFAULT 'pending',
    -- External references
    stripe_payment_intent_id VARCHAR(255),
    stripe_charge_id VARCHAR(255),
    gateway_response JSONB, -- raw response from payment gateway
    -- PIX-specific fields
    pix_qr_code     TEXT,
    pix_copy_paste  TEXT,
    pix_expiration  TIMESTAMPTZ,
    -- Boleto-specific fields
    boleto_url      TEXT,
    boleto_barcode  TEXT,
    boleto_due_date DATE,
    -- Refund
    refunded_amount_cents BIGINT DEFAULT 0,
    refund_reason   TEXT,
    refunded_at     TIMESTAMPTZ,
    -- Description
    description     TEXT,
    -- NF-e (Brazilian invoice)
    nfe_number      VARCHAR(50),
    nfe_url         TEXT,
    -- Metadata
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE billing.payments IS 'Payment records for subscriptions and credit purchases. Supports PIX, boleto, credit card.';
COMMENT ON COLUMN billing.payments.amount_cents IS 'Payment amount in BRL centavos.';
COMMENT ON COLUMN billing.payments.nfe_number IS 'Brazilian Nota Fiscal Eletronica number for tax compliance.';

CREATE INDEX idx_payments_org ON billing.payments(org_id);
CREATE INDEX idx_payments_user ON billing.payments(user_id);
CREATE INDEX idx_payments_subscription ON billing.payments(subscription_id);
CREATE INDEX idx_payments_status ON billing.payments(status);
CREATE INDEX idx_payments_stripe ON billing.payments(stripe_payment_intent_id);
CREATE INDEX idx_payments_created ON billing.payments(created_at);

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON billing.payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- INVOICES
-- ============================================================================

CREATE TABLE billing.invoices (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    subscription_id UUID REFERENCES billing.subscriptions(id),
    payment_id      UUID REFERENCES billing.payments(id),
    -- Invoice details
    invoice_number  VARCHAR(50) NOT NULL UNIQUE,
    status          invoice_status NOT NULL DEFAULT 'draft',
    -- Amounts (BRL centavos)
    subtotal_cents  BIGINT NOT NULL DEFAULT 0,
    discount_cents  BIGINT NOT NULL DEFAULT 0,
    tax_cents       BIGINT NOT NULL DEFAULT 0,
    total_cents     BIGINT NOT NULL DEFAULT 0,
    -- Dates
    issued_at       TIMESTAMPTZ,
    due_date        DATE,
    paid_at         TIMESTAMPTZ,
    -- Period
    period_start    TIMESTAMPTZ,
    period_end      TIMESTAMPTZ,
    -- Line items stored as JSONB
    line_items      JSONB NOT NULL DEFAULT '[]'::jsonb,
    -- PDF
    pdf_url         TEXT,
    -- External
    stripe_invoice_id VARCHAR(255),
    -- NF-e
    nfe_number      VARCHAR(50),
    nfe_url         TEXT,
    -- Customer details snapshot (for historical accuracy)
    billing_details JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE billing.invoices IS 'Invoices generated for subscriptions and one-time purchases.';
COMMENT ON COLUMN billing.invoices.line_items IS 'JSON array of line items: [{description, quantity, unit_price_cents, total_cents}].';
COMMENT ON COLUMN billing.invoices.billing_details IS 'Snapshot of customer billing info at time of invoice: name, address, CNPJ/CPF, etc.';

CREATE INDEX idx_invoices_org ON billing.invoices(org_id);
CREATE INDEX idx_invoices_subscription ON billing.invoices(subscription_id);
CREATE INDEX idx_invoices_status ON billing.invoices(status);
CREATE INDEX idx_invoices_number ON billing.invoices(invoice_number);
CREATE INDEX idx_invoices_created ON billing.invoices(created_at);

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON billing.invoices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- PAYMENT METHODS (stored cards, etc.)
-- ============================================================================

CREATE TABLE billing.payment_methods (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- Method type
    method_type     payment_method_type NOT NULL,
    -- Card details (masked)
    card_brand      VARCHAR(20), -- visa, mastercard, etc.
    card_last_four  VARCHAR(4),
    card_exp_month  SMALLINT,
    card_exp_year   SMALLINT,
    -- Status
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    -- External
    stripe_payment_method_id VARCHAR(255),
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE billing.payment_methods IS 'Stored payment methods (tokenized via Stripe). No raw card data stored.';

CREATE INDEX idx_payment_methods_user ON billing.payment_methods(user_id);
CREATE INDEX idx_payment_methods_org ON billing.payment_methods(org_id);

CREATE TRIGGER trg_payment_methods_updated_at
    BEFORE UPDATE ON billing.payment_methods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- BILLING ALERTS
-- ============================================================================

CREATE TABLE billing.billing_alerts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    -- Alert configuration
    alert_type      VARCHAR(50) NOT NULL, -- 'credit_low', 'credit_depleted', 'payment_failed', 'invoice_due'
    threshold_value BIGINT, -- e.g., 100 credits remaining
    -- Status
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMPTZ,
    -- Notification channels
    notify_email    BOOLEAN NOT NULL DEFAULT TRUE,
    notify_webhook  BOOLEAN NOT NULL DEFAULT FALSE,
    webhook_url     TEXT,
    -- Metadata
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_billing_alert UNIQUE (org_id, alert_type)
);

COMMENT ON TABLE billing.billing_alerts IS 'Configurable billing alerts: low credits, failed payments, upcoming invoices.';

CREATE TRIGGER trg_billing_alerts_updated_at
    BEFORE UPDATE ON billing.billing_alerts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
