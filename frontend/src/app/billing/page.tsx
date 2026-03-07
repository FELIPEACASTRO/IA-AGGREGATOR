'use client';

import { cn } from '@/lib/cn';
import { AppLayout } from '@/components/app/app-layout';
import { useEffect, useState } from 'react';
import { useTranslations } from 'next-intl';
import {
  Activity,
  AlertTriangle,
  Calendar,
  Check,
  CreditCard,
  Shield,
} from 'lucide-react';

type BillingPlan = {
  id: 'starter' | 'pro' | 'enterprise';
  name: string;
  price: string;
  desc: string;
  tokens: number;
  models: number;
  current: boolean;
  features: string[];
};

type WeeklyUsage = { day: string; tokens: number };

type BillingPayload = {
  plans: BillingPlan[];
  monthlyUsage: WeeklyUsage[];
  current: {
    tokensUsed: number;
    monthlyLimit: number;
    pct: number;
    estimatedFromRuns: boolean;
  };
};

export default function BillingPage() {
  const t = useTranslations();
  const [data, setData] = useState<BillingPayload | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadBilling = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/billing/usage', { cache: 'no-store' });
      const payload = (await response.json()) as { success: boolean; data?: BillingPayload; message?: string };
      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message || t('billing.error'));
      }
      setData(payload.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : t('billing.error'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBilling();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const plans = data?.plans ?? [];
  const monthlyUsage = data?.monthlyUsage ?? [];
  const tokensUsed = data?.current.tokensUsed ?? 0;
  const monthlyLimit = data?.current.monthlyLimit ?? 50000;
  const pct = data?.current.pct ?? 0;
  const currentPlan = plans.find((p) => p.current) ?? plans[0];
  const maxTokens = Math.max(1, ...monthlyUsage.map((i) => i.tokens));

  return (
    <AppLayout>
      <div className="mx-auto max-w-4xl px-6 py-8 space-y-8">
        <div>
          <h1 className="text-[24px] font-semibold text-[var(--foreground)]">{t('billing.title')}</h1>
          <p className="mt-1 text-[14px] text-[var(--muted-foreground)]">{t('billing.subtitle')}</p>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-16">
            <div className="flex gap-1.5">
              <span className="pulse-dot" />
              <span className="pulse-dot" />
              <span className="pulse-dot" />
            </div>
          </div>
        ) : error ? (
          <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-6 text-center">
            <p className="text-[14px] text-[var(--destructive)]">{error}</p>
            <button
              onClick={loadBilling}
              className="mt-3 rounded-[var(--radius-md)] border border-[var(--border)] px-4 py-2 text-[13px] font-medium text-[var(--foreground)] hover:bg-[var(--surface-hover)]"
            >
              {t('billing.retry')}
            </button>
          </div>
        ) : (
          <>
            {/* Stats */}
            <div className="grid gap-3 md:grid-cols-3">
              <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-4">
                <CreditCard className="h-5 w-5 text-[var(--muted-foreground)]" />
                <p className="mt-3 text-[20px] font-semibold text-[var(--foreground)]">{currentPlan?.name ?? '-'}</p>
                <p className="mt-0.5 text-[12px] text-[var(--muted-foreground)]">{t('billing.currentPlan')}</p>
              </div>
              <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-4">
                <Activity className="h-5 w-5 text-[var(--muted-foreground)]" />
                <p className="mt-3 text-[20px] font-semibold text-[var(--foreground)]">{(tokensUsed / 1000).toFixed(1)}k</p>
                <p className="mt-0.5 text-[12px] text-[var(--muted-foreground)]">{t('billing.tokensUsed')}</p>
              </div>
              <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-4">
                <Calendar className="h-5 w-5 text-[var(--muted-foreground)]" />
                <p className="mt-3 text-[20px] font-semibold text-[var(--foreground)]">18 dias</p>
                <p className="mt-0.5 text-[12px] text-[var(--muted-foreground)]">{t('billing.resetIn')}</p>
              </div>
            </div>

            {/* Usage bar */}
            <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-5">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-[15px] font-semibold text-[var(--foreground)]">Uso de tokens</h2>
                {pct >= 80 ? (
                  <span className="inline-flex items-center gap-1 rounded-[var(--radius-full)] bg-[var(--warning-light)] px-2.5 py-1 text-[11px] font-medium text-[var(--warning)]">
                    <AlertTriangle className="h-3 w-3" /> {pct}% usado
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 rounded-[var(--radius-full)] bg-[var(--success-light)] px-2.5 py-1 text-[11px] font-medium text-[var(--success)]">
                    <Shield className="h-3 w-3" /> {t('billing.healthy')}
                  </span>
                )}
              </div>
              <div className="mt-4 h-2.5 w-full overflow-hidden rounded-[var(--radius-full)] bg-[var(--surface-hover)]">
                <div
                  className={cn(
                    'h-full rounded-[var(--radius-full)] transition-all',
                    pct >= 90 ? 'bg-[var(--destructive)]' : pct >= 70 ? 'bg-[var(--warning)]' : 'bg-[var(--success)]',
                  )}
                  style={{ width: `${Math.min(100, pct)}%` }}
                />
              </div>
              <div className="mt-2 flex justify-between text-[12px] text-[var(--muted-foreground)]">
                <span>{tokensUsed.toLocaleString('pt-BR')} usados</span>
                <span>{(monthlyLimit - tokensUsed).toLocaleString('pt-BR')} restantes</span>
              </div>
            </div>

            {/* Weekly chart */}
            {monthlyUsage.length > 0 && (
              <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-5">
                <h2 className="text-[15px] font-semibold text-[var(--foreground)]">Consumo semanal</h2>
                <div className="mt-4 flex h-32 items-end gap-2">
                  {monthlyUsage.map((item) => (
                    <div key={item.day} className="flex flex-1 flex-col items-center gap-2">
                      <div className="flex h-full w-full items-end justify-center">
                        <div
                          className="w-full max-w-[32px] rounded-t-[var(--radius-sm)] bg-[var(--accent)]"
                          style={{ height: `${Math.max(8, Math.round((item.tokens / maxTokens) * 100))}%` }}
                          title={`${item.day}: ${item.tokens.toLocaleString('pt-BR')} tokens`}
                        />
                      </div>
                      <span className="text-[11px] text-[var(--muted-foreground)]">{item.day}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Plans */}
            <div>
              <h2 className="text-[16px] font-semibold text-[var(--foreground)] mb-4">{t('billing.availablePlans')}</h2>
              <div className="grid gap-4 md:grid-cols-3">
                {plans.map((plan) => (
                  <div
                    key={plan.id}
                    className={cn(
                      'relative rounded-[var(--radius-lg)] border p-5',
                      plan.current
                        ? 'border-[var(--accent)] bg-[var(--accent-light)]'
                        : 'border-[var(--border)] bg-[var(--surface)]',
                    )}
                  >
                    {plan.current && (
                      <span className="absolute right-4 top-4 rounded-[var(--radius-full)] bg-[var(--accent)] px-2.5 py-0.5 text-[11px] font-medium text-white">
                        {t('billing.current')}
                      </span>
                    )}
                    <h3 className="text-[16px] font-semibold text-[var(--foreground)]">{plan.name}</h3>
                    <p className="mt-1 text-[22px] font-semibold text-[var(--foreground)]">{plan.price}</p>
                    <p className="mt-0.5 text-[12px] text-[var(--muted-foreground)]">{plan.desc}</p>
                    <ul className="mt-4 space-y-2">
                      {plan.features.map((f) => (
                        <li key={f} className="flex items-start gap-2 text-[13px] text-[var(--muted-foreground)]">
                          <Check className="mt-0.5 h-3.5 w-3.5 shrink-0 text-[var(--success)]" />
                          <span>{f}</span>
                        </li>
                      ))}
                    </ul>
                    {plan.current ? (
                      <div className="mt-5 inline-flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] px-3 py-2 text-[12px] font-medium text-[var(--muted-foreground)]">
                        <Shield className="h-3.5 w-3.5" /> Plano atual
                      </div>
                    ) : (
                      <button className="mt-5 w-full rounded-[var(--radius-md)] bg-[var(--primary)] px-4 py-2.5 text-[13px] font-medium text-[var(--primary-foreground)] hover:opacity-90 transition-opacity">
                        {plan.id === 'enterprise' ? 'Falar com vendas' : t('billing.upgrade')}
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  );
}
