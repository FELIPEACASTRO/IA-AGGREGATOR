'use client';

import { AppShell } from '@/components/app/app-shell';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';
import { ProgressBar } from '@/components/ui/progress-bar';
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

const plans = [
  {
    id: 'starter',
    name: 'Starter',
    price: 'Gratis',
    desc: 'Ideal para explorar e validar',
    tokens: 50000,
    models: 5,
    current: true,
    features: ['5 modelos disponiveis', '50k tokens/mes', 'Historico 30 dias', 'Suporte por e-mail'],
    gradient: undefined,
  },
  {
    id: 'pro',
    name: 'Pro',
    price: 'R$ 49/mes',
    desc: 'Para profissionais e equipes',
    tokens: 500000,
    models: 13,
    current: false,
    features: ['13+ modelos disponiveis', '500k tokens/mes', 'Historico ilimitado', 'Canvas Mode', 'Suporte prioritario', 'API access'],
    gradient: 'var(--brand-gradient)',
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    price: 'Personalizado',
    desc: 'Escala e seguranca corporativa',
    tokens: -1,
    models: 13,
    current: false,
    features: ['Tokens ilimitados', 'SSO / SAML', 'SLA dedicado', 'Integracoes custom', 'Treinamento da equipe'],
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
  const maxTokens = Math.max(...monthlyUsage.map((item) => item.tokens));
  const currentPlan = plans.find((plan) => plan.current) ?? plans[0];

  return (
    <AppShell title="Plano e uso" subtitle="Monitore seu consumo e faca upgrade quando precisar">
      <div className="space-y-6 py-6">
        <section className="lume-panel rounded-[var(--radius-2xl)] p-5 md:p-6">
          <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr] xl:items-start">
            <div>
              <span className="lume-kicker">
                <Sparkles className="h-3.5 w-3.5" /> Revenue workspace
              </span>
              <h2 className="mt-5 max-w-3xl text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">
                Controle de plano, consumo e capacidade em uma camada executiva unica.
              </h2>
              <p className="mt-3 max-w-2xl text-[var(--text-sm)] text-[var(--muted-foreground)]">
                A referencia premium aqui e simples: hierarquia clara, status atual evidente e CTA de upgrade bem colocado. O usuario precisa entender em segundos quanto pode consumir e qual e o proximo nivel.
              </p>
              <div className="mt-5 grid gap-4 sm:grid-cols-3">
                <UsageStat label="Plano atual" value={currentPlan.name} sub={currentPlan.desc} icon={CreditCard} iconClass="text-[var(--brand-primary)]" />
                <UsageStat label="Tokens usados" value={`${(tokensUsed / 1000).toFixed(1)}k`} sub={`de ${(MONTHLY_LIMIT / 1000).toFixed(0)}k disponiveis`} icon={Activity} iconClass="text-[var(--warning)]" />
                <UsageStat label="Reset em" value="18 dias" sub="ciclo mensal em andamento" icon={Calendar} iconClass="text-[var(--success)]" />
              </div>
            </div>

            <div className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5 shadow-[var(--shadow-sm)]">
              <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Saude do consumo</p>
              <div className="mt-4">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Uso de tokens este mes</h3>
                  {pct >= 80 ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-[var(--warning)]/15 px-2.5 py-1 text-[0.72rem] font-semibold text-[var(--warning)]">
                      <AlertTriangle className="h-3.5 w-3.5" /> {pct}% usado
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 rounded-full bg-[var(--success)]/15 px-2.5 py-1 text-[0.72rem] font-semibold text-[var(--success)]">
                      <Shield className="h-3.5 w-3.5" /> Saudavel
                    </span>
                  )}
                </div>
                <ProgressBar value={pct} size="lg" warnThreshold={70} dangerThreshold={90} className="mt-4" />
                <div className="mt-3 flex items-center justify-between text-[var(--text-xs)] text-[var(--muted-foreground)]">
                  <span>{tokensUsed.toLocaleString('pt-BR')} tokens usados</span>
                  <span>{(MONTHLY_LIMIT - tokensUsed).toLocaleString('pt-BR')} restantes</span>
                </div>
              </div>
              <div className="mt-5 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.52)] p-4">
                <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Leitura rapida</p>
                <ul className="mt-3 space-y-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">
                  <li className="flex items-center gap-2"><TrendingUp className="h-4 w-4 text-[var(--brand-primary)]" /> consumo abaixo da zona de risco</li>
                  <li className="flex items-center gap-2"><Zap className="h-4 w-4 text-[var(--brand-secondary)]" /> upgrade recomendado so para fluxos multi-equipe</li>
                  <li className="flex items-center gap-2"><BarChart3 className="h-4 w-4 text-[var(--success)]" /> uso semanal concentrado em dias uteis</li>
                </ul>
              </div>
            </div>
          </div>
        </section>

        <section className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
            className="lume-panel rounded-[var(--radius-2xl)] p-5"
          >
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Tendencia semanal</p>
                <h3 className="mt-2 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Distribuicao de consumo</h3>
              </div>
              <span className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] px-3 py-1.5 text-[var(--text-xs)] text-[var(--muted-foreground)]">
                <TrendingUp className="h-3.5 w-3.5 text-[var(--brand-primary)]" /> ultimos 7 dias
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
                <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">Equipes compartilhando prompts, historico ilimitado e uso recorrente de canvas mode.</p>
              </div>
              <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
                <p className="text-[var(--text-sm)] font-semibold text-[var(--foreground)]">Quando falar com vendas</p>
                <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">Governanca corporativa, SSO e integracoes dedicadas para operacao em escala.</p>
              </div>
            </div>
          </motion.div>
        </section>

        <section>
          <div className="mb-4 flex items-center gap-2">
            <Zap className="h-4.5 w-4.5 text-[var(--brand-primary)]" />
            <h2 className="text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Planos disponiveis</h2>
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
                    Plano atual
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
                      <Shield className="h-4 w-4" /> Plano ativo
                    </div>
                  ) : (
                    <button
                      className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-[var(--radius-pill)] px-5 py-3 text-[var(--text-xs)] font-semibold text-white shadow-[var(--shadow-brand)] hover:-translate-y-0.5"
                      style={{ background: plan.gradient ?? 'var(--brand-gradient)' }}
                    >
                      {plan.id === 'enterprise' ? 'Falar com vendas' : 'Fazer upgrade'}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </AppShell>
  );
}
