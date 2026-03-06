'use client';

import { AppShell } from '@/components/app/app-shell';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';
import { ProgressBar } from '@/components/ui/progress-bar';
import { PageSection, PageStack } from '@/components/app/page-blueprint';
import { useEffect, useState } from 'react';
import { useTranslations } from 'next-intl';
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Calendar,
  Check,
  CreditCard,
  Shield,
  Sparkles,
  TrendingUp,
  Zap,
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
  gradient?: string;
};

type WeeklyUsage = {
  day: string;
  tokens: number;
};

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

function UsageStat({ label, value, sub, icon: Icon, iconClass }: { label: string; value: string; sub: string; icon: React.ElementType; iconClass: string }) {
  return (
    <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">{label}</p>
          <p className="mt-2 text-[var(--text-2xl)] font-semibold text-[var(--foreground)]">{value}</p>
          <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{sub}</p>
        </div>
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] border border-[var(--border)] bg-[rgba(255,255,255,0.03)]">
          <Icon className={cn('h-5 w-5', iconClass)} />
        </span>
      </div>
    </div>
  );
}

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
  const currentPlan = plans.find((plan) => plan.current) ?? plans[0];
  const maxTokens = Math.max(1, ...monthlyUsage.map((item) => item.tokens));

  return (
    <AppShell title={t('billing.title')} subtitle={t('billing.subtitle')}>
      <PageStack>
        {loading ? (
          <PageSection className="p-6">
            <p className="text-[var(--text-sm)] text-[var(--muted-foreground)]">{t('billing.loading')}</p>
          </PageSection>
        ) : error ? (
          <PageSection className="p-6">
            <p className="text-[var(--text-sm)] text-[var(--destructive)]">{error}</p>
            <button
              onClick={loadBilling}
              className="mt-3 rounded-[var(--radius-pill)] border border-[var(--border)] px-4 py-2 text-[var(--text-xs)] font-semibold text-[var(--foreground)] hover:border-[var(--brand-primary)]"
            >
              {t('billing.retry')}
            </button>
          </PageSection>
        ) : (
          <>
            <PageSection className="p-5 md:p-6">
              <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr] xl:items-start">
                <div>
                  <span className="lume-kicker">
                    <Sparkles className="h-3.5 w-3.5" /> Revenue workspace
                  </span>
                  <h2 className="mt-5 max-w-3xl text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">
                    Controle de plano, consumo e capacidade em uma camada executiva única.
                  </h2>
                  <p className="mt-3 max-w-2xl text-[var(--text-sm)] text-[var(--muted-foreground)]">
                    O objetivo é leitura imediata: status do plano, consumo atual, capacidade restante e próximo passo com clareza.
                  </p>
                  <div className="mt-5 grid gap-4 sm:grid-cols-3">
                    <UsageStat label={t('billing.currentPlan')} value={currentPlan?.name ?? '-'} sub={currentPlan?.desc ?? '-'} icon={CreditCard} iconClass="text-[var(--brand-primary)]" />
                    <UsageStat label={t('billing.tokensUsed')} value={`${(tokensUsed / 1000).toFixed(1)}k`} sub={`de ${(monthlyLimit / 1000).toFixed(0)}k disponíveis`} icon={Activity} iconClass="text-[var(--warning)]" />
                    <UsageStat label={t('billing.resetIn')} value="18 dias" sub="ciclo mensal em andamento" icon={Calendar} iconClass="text-[var(--success)]" />
                  </div>
                </div>

                <div className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5 shadow-[var(--shadow-sm)]">
                  <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">{t('billing.usageHealth')}</p>
                  <div className="mt-4">
                    <div className="flex items-center justify-between gap-3">
                      <h3 className="text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Uso de tokens este mês</h3>
                      {pct >= 80 ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-[var(--warning)]/15 px-2.5 py-1 text-[0.72rem] font-semibold text-[var(--warning-foreground)]">
                          <AlertTriangle className="h-3.5 w-3.5" /> {t('billing.used', { value: pct })}
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-[var(--success)]/15 px-2.5 py-1 text-[0.72rem] font-semibold text-[var(--success)]">
                          <Shield className="h-3.5 w-3.5" /> {t('billing.healthy')}
                        </span>
                      )}
                    </div>
                    <ProgressBar value={pct} size="lg" warnThreshold={70} dangerThreshold={90} className="mt-4" />
                    <div className="mt-3 flex items-center justify-between text-[var(--text-xs)] text-[var(--muted-foreground)]">
                      <span>{t('billing.usedTokens', { value: tokensUsed.toLocaleString('pt-BR') })}</span>
                      <span>{t('billing.remaining', { value: (monthlyLimit - tokensUsed).toLocaleString('pt-BR') })}</span>
                    </div>
                  </div>
                  <div className="mt-5 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.52)] p-4">
                    <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Leitura rápida</p>
                    <ul className="mt-3 space-y-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">
                      <li className="flex items-center gap-2"><TrendingUp className="h-4 w-4 text-[var(--brand-primary)]" /> consumo abaixo da zona de risco</li>
                      <li className="flex items-center gap-2"><Zap className="h-4 w-4 text-[var(--brand-secondary)]" /> upgrade recomendado só para fluxos multi-equipe</li>
                      <li className="flex items-center gap-2"><BarChart3 className="h-4 w-4 text-[var(--success)]" /> uso semanal concentrado em dias úteis</li>
                    </ul>
                  </div>
                </div>
              </div>
            </PageSection>

            <section className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
              <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.25 }}
                className="lume-panel rounded-[var(--radius-2xl)] p-5"
              >
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">{t('billing.weeklyTrend')}</p>
                    <h3 className="mt-2 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Distribuição de consumo</h3>
                  </div>
                  <span className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] px-3 py-1.5 text-[var(--text-xs)] text-[var(--muted-foreground)]">
                    <TrendingUp className="h-3.5 w-3.5 text-[var(--brand-primary)]" /> {t('billing.last7Days')}
                  </span>
                </div>

                <div className="mt-6 flex h-48 items-end gap-2">
                  {monthlyUsage.map((item) => (
                    <div key={item.day} className="flex flex-1 flex-col items-center gap-2">
                      <div className="relative flex h-full w-full items-end rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-2">
                        <div
                          className="w-full rounded-[var(--radius-lg)] bg-[var(--brand-gradient)] shadow-[var(--shadow-brand)]"
                          style={{ height: `${Math.max(10, Math.round((item.tokens / maxTokens) * 150))}px` }}
                          title={`${item.day}: ${item.tokens.toLocaleString('pt-BR')} tokens`}
                        />
                      </div>
                      <span className="text-[0.72rem] font-semibold text-[var(--muted-foreground)]">{item.day}</span>
                    </div>
                  ))}
                </div>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.25, delay: 0.04 }}
                className="lume-panel-soft rounded-[var(--radius-2xl)] p-5"
              >
                <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Uso recomendado</p>
                <h3 className="mt-2 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Seus limites hoje cobrem bem o fluxo principal.</h3>
                <div className="mt-5 space-y-3">
                  <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
                    <p className="text-[var(--text-sm)] font-semibold text-[var(--foreground)]">Quando subir para Pro</p>
                    <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">Equipes compartilhando prompts, histórico ilimitado e uso recorrente de canvas mode.</p>
                  </div>
                  <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
                    <p className="text-[var(--text-sm)] font-semibold text-[var(--foreground)]">Quando falar com vendas</p>
                    <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">Governança corporativa, SSO e integrações dedicadas para operação em escala.</p>
                  </div>
                </div>
              </motion.div>
            </section>

            <section>
              <div className="mb-4 flex items-center gap-2">
                <Zap className="h-4.5 w-4.5 text-[var(--brand-primary)]" />
                <h2 className="text-[var(--text-lg)] font-semibold text-[var(--foreground)]">{t('billing.availablePlans')}</h2>
              </div>
              <div className="grid gap-4 md:grid-cols-3">
                {plans.map((plan) => (
                  <div
                    key={plan.id}
                    className={cn(
                      'relative overflow-hidden rounded-[var(--radius-2xl)] border p-5',
                      plan.current ? 'border-[var(--brand-primary)] bg-[rgba(96,115,255,0.08)] shadow-[var(--shadow-brand)]' : 'lume-panel-soft',
                    )}
                  >
                    {plan.current ? (
                      <span className="absolute right-4 top-4 rounded-full bg-[var(--brand-primary)] px-3 py-1 text-[0.68rem] font-semibold text-white">
                        {t('billing.current')}
                      </span>
                    ) : null}
                    <div className="relative z-[1]">
                      <h3 className="text-[var(--text-lg)] font-semibold text-[var(--foreground)]">{plan.name}</h3>
                      <p className="mt-2 text-[var(--text-2xl)] font-semibold text-[var(--foreground)]">{plan.price}</p>
                      <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{plan.desc}</p>
                      <ul className="mt-5 space-y-3">
                        {plan.features.map((feature) => (
                          <li key={feature} className="flex items-start gap-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">
                            <Check className="mt-0.5 h-4 w-4 shrink-0 text-[var(--success)]" />
                            <span>{feature}</span>
                          </li>
                        ))}
                      </ul>
                      {plan.current ? (
                        <div className="mt-6 inline-flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-4 py-2.5 text-[var(--text-xs)] font-semibold text-[var(--muted-foreground)]">
                          <Shield className="h-4 w-4" /> {t('billing.current')}
                        </div>
                      ) : (
                        <button
                          className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-[var(--radius-pill)] px-5 py-3 text-[var(--text-xs)] font-semibold text-white shadow-[var(--shadow-brand)] hover:-translate-y-0.5"
                          style={{ background: plan.gradient ?? 'var(--brand-gradient)' }}
                        >
                          {plan.id === 'enterprise' ? 'Falar com vendas' : t('billing.upgrade')}
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </>
        )}
      </PageStack>
    </AppShell>
  );
}
