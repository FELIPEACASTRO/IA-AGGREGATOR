-- ============================================================================
-- V5: Billing Module Tables
-- ============================================================================

SET search_path TO billing, auth, public;

-- Plans
CREATE TABLE billing.plans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slug            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    tier            plan_tier NOT NULL,
    price_display   VARCHAR(50) NOT NULL,
    price_cents     INTEGER NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'BRL',
    billing_cycle   billing_cycle NOT NULL DEFAULT 'monthly',
    description     TEXT,
    token_limit     BIGINT NOT NULL DEFAULT 0,
    model_count     INTEGER NOT NULL DEFAULT 0,
    features        JSONB NOT NULL DEFAULT '[]'::jsonb,
    gradient        VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_plans_updated_at
    BEFORE UPDATE ON billing.plans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed default plans
INSERT INTO billing.plans (slug, name, tier, price_display, price_cents, description, token_limit, model_count, features, sort_order) VALUES
    ('starter', 'Starter', 'free', 'Grátis', 0, 'Ideal para explorar e validar', 50000, 5, '["5 modelos disponíveis", "50k tokens/mês", "Histórico 30 dias", "Suporte por e-mail"]'::jsonb, 1),
    ('pro', 'Pro', 'pro', 'R$ 49/mês', 4900, 'Para profissionais e equipes', 500000, 13, '["13+ modelos disponíveis", "500k tokens/mês", "Histórico ilimitado", "Canvas Mode", "Suporte prioritário", "API access"]'::jsonb, 2),
    ('enterprise', 'Enterprise', 'enterprise', 'Personalizado', 0, 'Escala e segurança corporativa', -1, 13, '["Tokens ilimitados", "SSO / SAML", "SLA dedicado", "Integrações customizadas", "Contato comercial"]'::jsonb, 3);

-- Subscriptions
CREATE TABLE billing.subscriptions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES auth.users(id),
    plan_id         UUID NOT NULL REFERENCES billing.plans(id),
    status          subscription_status NOT NULL DEFAULT 'active',
    current_period_start TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    current_period_end   TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '1 month'),
    cancel_at       TIMESTAMPTZ,
    canceled_at     TIMESTAMPTZ,
    trial_start     TIMESTAMPTZ,
    trial_end       TIMESTAMPTZ,
    stripe_subscription_id VARCHAR(255),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_org ON billing.subscriptions(org_id);
CREATE INDEX idx_subscriptions_user ON billing.subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON billing.subscriptions(status) WHERE status = 'active';

CREATE TRIGGER trg_subscriptions_updated_at
    BEFORE UPDATE ON billing.subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Usage Records
CREATE TABLE billing.usage_records (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    user_id         UUID REFERENCES auth.users(id),
    subscription_id UUID REFERENCES billing.subscriptions(id),
    metric          VARCHAR(100) NOT NULL,
    amount          BIGINT NOT NULL DEFAULT 0,
    unit            VARCHAR(50) NOT NULL DEFAULT 'tokens',
    model_used      VARCHAR(100),
    provider_used   VARCHAR(100),
    period          VARCHAR(7),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usage_records_org ON billing.usage_records(org_id);
CREATE INDEX idx_usage_records_period ON billing.usage_records(period);
CREATE INDEX idx_usage_records_recorded ON billing.usage_records(recorded_at);
CREATE INDEX idx_usage_records_metric ON billing.usage_records(metric);

-- Credit Transactions
CREATE TABLE billing.credit_transactions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    user_id         UUID REFERENCES auth.users(id),
    tx_type         credit_tx_type NOT NULL,
    amount          BIGINT NOT NULL,
    balance_after   BIGINT NOT NULL,
    description     TEXT,
    related_task_id UUID,
    related_usage_id UUID REFERENCES billing.usage_records(id),
    stripe_payment_intent_id VARCHAR(255),
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_credit_tx_org ON billing.credit_transactions(org_id);
CREATE INDEX idx_credit_tx_type ON billing.credit_transactions(tx_type);
CREATE INDEX idx_credit_tx_created ON billing.credit_transactions(created_at);

-- Invoices
CREATE TABLE billing.invoices (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    subscription_id UUID REFERENCES billing.subscriptions(id),
    status          invoice_status NOT NULL DEFAULT 'draft',
    amount_cents    INTEGER NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'BRL',
    period_start    TIMESTAMPTZ NOT NULL,
    period_end      TIMESTAMPTZ NOT NULL,
    due_date        TIMESTAMPTZ,
    paid_at         TIMESTAMPTZ,
    stripe_invoice_id VARCHAR(255),
    pdf_url         TEXT,
    line_items      JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_org ON billing.invoices(org_id);
CREATE INDEX idx_invoices_status ON billing.invoices(status);

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON billing.invoices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Budgets
CREATE TABLE billing.budgets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id          UUID NOT NULL REFERENCES auth.organizations(id),
    name            VARCHAR(100) NOT NULL,
    monthly_limit_cents INTEGER NOT NULL,
    alert_threshold_pct SMALLINT NOT NULL DEFAULT 80,
    action          VARCHAR(20) NOT NULL DEFAULT 'ALERT',
    current_spend_cents INTEGER NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_budgets_org ON billing.budgets(org_id);

CREATE TRIGGER trg_budgets_updated_at
    BEFORE UPDATE ON billing.budgets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
