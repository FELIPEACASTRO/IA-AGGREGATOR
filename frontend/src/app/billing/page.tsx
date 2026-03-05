'use client';

import { AppShell } from '@/components/app/app-shell';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';
import { ProgressBar } from '@/components/ui/progress-bar';
import {
  Activity, BarChart3, CreditCard, Zap, Shield, Check,
  TrendingUp, Calendar, AlertTriangle,
} from 'lucide-react';

const plans = [
  {
    id: 'starter',
    name: 'Starter',
    price: 'Grátis',
    desc: 'Ideal para explorar e validar',
    tokens: 50000,
    models: 5,
    current: true,
    features: ['5 modelos disponíveis', '50k tokens/mês', 'Histórico 30 dias', 'Suporte por e-mail'],
    gradient: undefined,
  },
  {
    id: 'pro',
    name: 'Pro',
    price: 'R$ 49/mês',
    desc: 'Para profissionais e equipes',
    tokens: 500000,
    models: 13,
    current: false,
    features: ['13+ modelos disponíveis', '500k tokens/mês', 'Histórico ilimitado', 'Canvas Mode', 'Suporte prioritário', 'API access'],
    gradient: 'var(--brand-gradient)',
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    price: 'Personalizado',
    desc: 'Escala e segurança corporativa',
    tokens: -1,
    models: 13,
    current: false,
    features: ['Tokens ilimitados', 'SSO / SAML', 'SLA dedicado', 'Integrações custom', 'Treinamento da equipe'],
    gradient: undefined,
  },
];

const monthlyUsage = [
  { day: 'Seg', tokens: 3200 },
  { day: 'Ter', tokens: 5400 },
  { day: 'Qua', tokens: 2100 },
  { day: 'Qui', tokens: 7800 },
  { day: 'Sex', tokens: 6300 },
  { day: 'Sab', tokens: 1200 },
  { day: 'Dom', tokens: 900 },
];

const MONTHLY_LIMIT = 50000;
const tokensUsed = 12400;
const pct = Math.round((tokensUsed / MONTHLY_LIMIT) * 100);

export default function BillingPage() {
  const maxTokens = Math.max(...monthlyUsage.map((d) => d.tokens));

  return (
    <AppShell title="Plano e uso" subtitle="Monitore seu consumo e faça upgrade quando precisar">
      <div className="py-6 space-y-6 max-w-5xl">

        {/* Current usage overview */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}
          className="grid gap-4 sm:grid-cols-3">
          {[
            { label: 'Plano atual', value: 'Starter', sub: 'Grátis para sempre', icon: CreditCard, color: 'text-[var(--brand-primary)]', bg: 'bg-[var(--brand-primary)]/10' },
            { label: 'Tokens usados', value: `${(tokensUsed / 1000).toFixed(1)}k`, sub: `de ${MONTHLY_LIMIT / 1000}k disponíveis`, icon: Activity, color: 'text-[var(--warning)]', bg: 'bg-[var(--warning)]/10' },
            { label: 'Reset em', value: '18 dias', sub: 'Ciclo mensal', icon: Calendar, color: 'text-[var(--success)]', bg: 'bg-[var(--success)]/10' },
          ].map((stat) => (
            <div key={stat.label} className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] p-5">
              <div className={cn('mb-3 inline-flex h-9 w-9 items-center justify-center rounded-[var(--radius-lg)]', stat.bg)}>
                <stat.icon className={cn('h-4.5 w-4.5', stat.color)} />
              </div>
              <p className="text-[var(--text-2xl)] font-bold tabular-nums">{stat.value}</p>
              <p className="text-[var(--text-xs)] text-[var(--muted-foreground)] mt-0.5">{stat.label}</p>
              <p className="text-[10px] text-[var(--subtle-foreground)] mt-0.5">{stat.sub}</p>
            </div>
          ))}
        </motion.div>

        {/* Token usage bar */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, delay: 0.05 }}
          className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] p-5">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold text-[var(--text-sm)] flex items-center gap-2">
              <BarChart3 className="h-4 w-4 text-[var(--muted-foreground)]" /> Uso de tokens este mês
            </h2>
            {pct >= 80 && (
              <span className="inline-flex items-center gap-1 rounded-full bg-[var(--warning)]/15 px-2.5 py-1 text-[10px] font-medium text-[var(--warning)]">
                <AlertTriangle className="h-3 w-3" /> {pct}% usado
              </span>
            )}
          </div>
          <ProgressBar value={pct} size="lg" warnThreshold={70} dangerThreshold={90} className="mb-2" />
          <div className="flex justify-between text-[var(--text-xs)] text-[var(--muted-foreground)]">
            <span>{tokensUsed.toLocaleString('pt-BR')} tokens usados</span>
            <span>{(MONTHLY_LIMIT - tokensUsed).toLocaleString('pt-BR')} restantes</span>
          </div>

          {/* Mini chart */}
          <div className="mt-5">
            <p className="text-[var(--text-xs)] text-[var(--muted-foreground)] mb-3 flex items-center gap-1.5">
              <TrendingUp className="h-3.5 w-3.5" /> Últimos 7 dias
            </p>
            <div className="flex items-end gap-1.5 h-16">
              {monthlyUsage.map((d) => (
                <div key={d.day} className="flex flex-1 flex-col items-center gap-1">
                  <div className="w-full rounded-t-sm bg-[var(--brand-primary)]/60 hover:bg-[var(--brand-primary)] transition-colors"
                    style={{ height: `${Math.max(4, Math.round((d.tokens / maxTokens) * 48))}px` }}
                    title={`${d.day}: ${d.tokens.toLocaleString()} tokens`} />
                  <span className="text-[10px] text-[var(--subtle-foreground)]">{d.day}</span>
                </div>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Plans */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, delay: 0.1 }}>
          <h2 className="font-semibold text-[var(--text-base)] mb-4 flex items-center gap-2">
            <Zap className="h-4 w-4 text-[var(--brand-primary)]" /> Planos disponíveis
          </h2>
          <div className="grid gap-4 md:grid-cols-3">
            {plans.map((plan) => (
              <div key={plan.id}
                className={cn('relative rounded-[var(--radius-xl)] border p-5 flex flex-col',
                  plan.current ? 'border-[var(--brand-primary)] shadow-[var(--shadow-brand)]' : 'border-[var(--border)] bg-[var(--surface-1)]')}>
                {plan.gradient && (
                  <div className="absolute inset-0 rounded-[var(--radius-xl)] opacity-5" style={{ background: plan.gradient }} />
                )}
                {plan.current && (
                  <span className="absolute -top-2.5 left-4 rounded-full bg-[var(--brand-primary)] px-2.5 py-0.5 text-[10px] font-bold text-white">
                    Plano atual
                  </span>
                )}
                <div className="mb-4">
                  <h3 className="font-bold text-[var(--text-base)]">{plan.name}</h3>
                  <p className="text-[var(--text-2xl)] font-bold mt-1">{plan.price}</p>
                  <p className="text-[var(--text-xs)] text-[var(--muted-foreground)] mt-0.5">{plan.desc}</p>
                </div>
                <ul className="space-y-2 flex-1 mb-5">
                  {plan.features.map((f) => (
                    <li key={f} className="flex items-center gap-2 text-[var(--text-xs)]">
                      <Check className="h-3.5 w-3.5 shrink-0 text-[var(--success)]" />
                      {f}
                    </li>
                  ))}
                </ul>
                {plan.current ? (
                  <div className="flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-4 py-2.5 text-[var(--text-xs)] font-medium text-[var(--muted-foreground)]">
                    <Shield className="h-3.5 w-3.5" /> Plano ativo
                  </div>
                ) : (
                  <button
                    className="rounded-[var(--radius-md)] py-2.5 text-[var(--text-xs)] font-semibold text-white hover:opacity-90 active:scale-[0.98] transition-all"
                    style={{ background: plan.gradient ?? 'var(--brand-gradient)' }}>
                    {plan.id === 'enterprise' ? 'Falar com vendas' : 'Fazer upgrade'}
                  </button>
                )}
              </div>
            ))}
          </div>
        </motion.div>
      </div>
    </AppShell>
  );
}
