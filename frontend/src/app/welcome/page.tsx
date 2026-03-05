'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { trackEvent } from '@/lib/analytics';
import { cn } from '@/lib/cn';
import {
  BarChart3, FileText, Target, Sparkles, Bot,
  ArrowRight, Check, Zap, Scale, Brain,
  MessageSquare, ChevronRight,
} from 'lucide-react';

// ─── Types ────────────────────────────────────────────────────────────────────

type Goal = 'analysis' | 'writing' | 'planning' | 'general';
type Tier = 'fast' | 'balanced' | 'powerful';

// ─── Data ─────────────────────────────────────────────────────────────────────

const goals = [
  {
    id: 'analysis' as Goal,
    label: 'Análise & Relatórios',
    desc: 'Extraia insights de dados e gere relatórios executivos.',
    icon: BarChart3,
    color: 'text-[var(--brand-primary)]',
    bg: 'bg-[var(--brand-primary)]/10',
    border: 'border-[var(--brand-primary)]',
    shadow: 'shadow-[0_0_0_2px_var(--brand-primary)]',
    suggestedModel: 'gpt-4.1-mini',
    prompts: [
      'Crie um resumo executivo com os 5 principais insights sobre:',
      'Analise estes dados e identifique tendências e anomalias:',
      'Compare estes dois períodos e explique as principais variações:',
    ],
  },
  {
    id: 'writing' as Goal,
    label: 'Escrita & Comunicação',
    desc: 'Redija e-mails, relatórios e conteúdo com tom profissional.',
    icon: FileText,
    color: 'text-[var(--brand-secondary)]',
    bg: 'bg-[var(--brand-secondary)]/10',
    border: 'border-[var(--brand-secondary)]',
    shadow: 'shadow-[0_0_0_2px_var(--brand-secondary)]',
    suggestedModel: 'claude-3-5-haiku',
    prompts: [
      'Escreva um e-mail profissional e objetivo sobre:',
      'Reescreva este texto tornando-o mais claro e persuasivo:',
      'Crie um comunicado formal para toda a empresa sobre:',
    ],
  },
  {
    id: 'planning' as Goal,
    label: 'Planejamento & Projetos',
    desc: 'Monte planos de ação, sprints e roadmaps estruturados.',
    icon: Target,
    color: 'text-[var(--success)]',
    bg: 'bg-[var(--success)]/10',
    border: 'border-[var(--success)]',
    shadow: 'shadow-[0_0_0_2px_var(--success)]',
    suggestedModel: 'gpt-4o-mini',
    prompts: [
      'Monte um plano de ação detalhado com etapas, donos e riscos para:',
      'Crie um roadmap de produto para os próximos 3 meses focado em:',
      'Divida este objetivo em milestones semanais e defina critérios de sucesso:',
    ],
  },
  {
    id: 'general' as Goal,
    label: 'Assistente Geral',
    desc: 'Pesquise, explore ideias e resolva qualquer desafio.',
    icon: Sparkles,
    color: 'text-[var(--warning)]',
    bg: 'bg-[var(--warning)]/10',
    border: 'border-[var(--warning)]',
    shadow: 'shadow-[0_0_0_2px_var(--warning)]',
    suggestedModel: 'deepseek-chat',
    prompts: [
      'Explique de forma simples e com exemplos práticos o conceito de:',
      'Quais são os 5 fatores mais críticos para o sucesso em:',
      'Compare as principais abordagens disponíveis para resolver:',
    ],
  },
];

const models = [
  { id: 'gpt-4o-mini',          label: 'GPT-4o mini',     provider: 'OpenAI',    tier: 'fast' as Tier,     color: '#10a37f' },
  { id: 'claude-3-5-haiku',     label: 'Claude Haiku',    provider: 'Anthropic', tier: 'fast' as Tier,     color: '#d4763b' },
  { id: 'gemini-1.5-flash',     label: 'Gemini Flash',    provider: 'Google',    tier: 'fast' as Tier,     color: '#4285F4' },
  { id: 'deepseek-chat',        label: 'DeepSeek Chat',   provider: 'DeepSeek',  tier: 'balanced' as Tier, color: '#6366f1' },
  { id: 'llama-3.1-70b-versatile', label: 'Llama 70B',   provider: 'Meta',      tier: 'balanced' as Tier, color: '#0667d0' },
  { id: 'command-r-plus',       label: 'Command R+',      provider: 'Cohere',    tier: 'powerful' as Tier, color: '#39d353' },
];

const tierConfig: Record<Tier, { icon: React.ElementType; label: string; color: string }> = {
  fast:     { icon: Zap,   label: 'Rápido',      color: 'text-[var(--success)]'       },
  balanced: { icon: Scale, label: 'Equilibrado',  color: 'text-[var(--warning)]'       },
  powerful: { icon: Brain, label: 'Poderoso',     color: 'text-[var(--brand-primary)]' },
};

const ONBOARDING_KEY = 'ia-onboarding-done';

const TOTAL_STEPS = 3;

// ─── Step indicators ──────────────────────────────────────────────────────────

function StepDots({ current, total }: { current: number; total: number }) {
  return (
    <div className="flex items-center gap-2">
      {Array.from({ length: total }, (_, i) => (
        <motion.div
          key={i}
          animate={{ width: i === current ? 24 : 8, background: i <= current ? 'var(--brand-primary)' : 'var(--surface-3)' }}
          transition={{ duration: 0.25 }}
          className="h-2 rounded-full"
        />
      ))}
    </div>
  );
}

// ─── Step 1: Goal selection ───────────────────────────────────────────────────

function StepGoal({ selected, onSelect }: { selected: Goal | null; onSelect: (g: Goal) => void }) {
  return (
    <motion.div key="goal" initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -40 }} transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
      className="space-y-4">
      <div>
        <h2 className="text-[var(--text-2xl)] font-bold tracking-tight">Qual é seu principal objetivo?</h2>
        <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">Vamos personalizar sua experiência com base no seu uso.</p>
      </div>
      <div className="grid grid-cols-2 gap-3">
        {goals.map((goal) => {
          const Icon = goal.icon;
          const active = selected === goal.id;
          return (
            <motion.button key={goal.id} type="button" onClick={() => onSelect(goal.id)}
              whileHover={{ y: -2 }} whileTap={{ scale: 0.97 }}
              className={cn(
                'relative flex flex-col items-start gap-3 rounded-[var(--radius-xl)] border p-4 text-left transition-all',
                active
                  ? `${goal.border} ${goal.shadow} bg-[var(--surface-2)]`
                  : 'border-[var(--border)] bg-[var(--surface-1)] hover:border-[var(--border-strong)] hover:bg-[var(--surface-2)]',
              )}>
              {active && (
                <motion.span initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: 'spring', stiffness: 400, damping: 20 }}
                  className="absolute right-3 top-3 inline-flex h-5 w-5 items-center justify-center rounded-full bg-[var(--brand-primary)]">
                  <Check className="h-3 w-3 text-white" />
                </motion.span>
              )}
              <span className={cn('inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-lg)]', goal.bg)}>
                <Icon className={cn('h-5 w-5', goal.color)} />
              </span>
              <div>
                <p className="text-[var(--text-sm)] font-semibold leading-tight">{goal.label}</p>
                <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)] leading-snug">{goal.desc}</p>
              </div>
            </motion.button>
          );
        })}
      </div>
    </motion.div>
  );
}

// ─── Step 2: Model selection ──────────────────────────────────────────────────

function StepModel({ selected, recommended, onSelect }: { selected: string | null; recommended: string; onSelect: (id: string) => void }) {
  return (
    <motion.div key="model" initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -40 }} transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
      className="space-y-4">
      <div>
        <h2 className="text-[var(--text-2xl)] font-bold tracking-tight">Escolha seu modelo favorito</h2>
        <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">Você pode trocar a qualquer momento no chat.</p>
      </div>
      <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3">
        {models.map((model) => {
          const active = (selected ?? recommended) === model.id;
          const TierIcon = tierConfig[model.tier].icon;
          return (
            <motion.button key={model.id} type="button" onClick={() => onSelect(model.id)}
              whileHover={{ y: -2 }} whileTap={{ scale: 0.97 }}
              className={cn(
                'flex flex-col items-start gap-2 rounded-[var(--radius-xl)] border p-4 text-left transition-all',
                active
                  ? 'border-[var(--brand-primary)] shadow-[0_0_0_2px_var(--brand-primary)] bg-[var(--surface-2)]'
                  : 'border-[var(--border)] bg-[var(--surface-1)] hover:border-[var(--border-strong)] hover:bg-[var(--surface-2)]',
              )}>
              <div className="flex w-full items-center justify-between">
                <div className="h-2.5 w-2.5 rounded-full" style={{ background: model.color }} />
                {active && (
                  <motion.span initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: 'spring', stiffness: 400, damping: 20 }}
                    className="inline-flex h-4 w-4 items-center justify-center rounded-full bg-[var(--brand-primary)]">
                    <Check className="h-2.5 w-2.5 text-white" />
                  </motion.span>
                )}
                {!active && model.id === recommended && (
                  <span className="rounded-full bg-[var(--brand-primary)]/15 px-2 py-0.5 text-[9px] font-medium text-[var(--brand-primary)]">
                    Recomendado
                  </span>
                )}
              </div>
              <div>
                <p className="text-[var(--text-sm)] font-semibold leading-tight">{model.label}</p>
                <p className="text-[10px] text-[var(--muted-foreground)] mt-0.5">{model.provider}</p>
              </div>
              <span className={cn('inline-flex items-center gap-1 text-[10px]', tierConfig[model.tier].color)}>
                <TierIcon className="h-3 w-3" /> {tierConfig[model.tier].label}
              </span>
            </motion.button>
          );
        })}
      </div>
    </motion.div>
  );
}

// ─── Step 3: First prompt ─────────────────────────────────────────────────────

function StepPrompt({
  goal, prompt, onChange,
}: {
  goal: Goal;
  prompt: string;
  onChange: (v: string) => void;
}) {
  const goalData = goals.find((g) => g.id === goal) ?? goals[0];
  return (
    <motion.div key="prompt" initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -40 }} transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
      className="space-y-4">
      <div>
        <h2 className="text-[var(--text-2xl)] font-bold tracking-tight">Tudo pronto! Qual é sua primeira pergunta?</h2>
        <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">Escolha um template ou escreva sua própria mensagem.</p>
      </div>

      {/* Suggested prompts */}
      <div className="space-y-2">
        {goalData.prompts.map((p) => (
          <button key={p} type="button" onClick={() => onChange(p)}
            className={cn(
              'w-full flex items-start gap-3 rounded-[var(--radius-lg)] border px-4 py-3 text-left transition-all',
              prompt === p
                ? 'border-[var(--brand-primary)] bg-[var(--brand-primary)]/5'
                : 'border-[var(--border)] bg-[var(--surface-1)] hover:border-[var(--border-strong)] hover:bg-[var(--surface-2)]',
            )}>
            <MessageSquare className={cn('mt-0.5 h-4 w-4 shrink-0', prompt === p ? 'text-[var(--brand-primary)]' : 'text-[var(--muted-foreground)]')} />
            <span className="text-[var(--text-sm)] leading-relaxed">{p}</span>
            {prompt === p && <Check className="ml-auto mt-0.5 h-4 w-4 shrink-0 text-[var(--brand-primary)]" />}
          </button>
        ))}
      </div>

      {/* Custom prompt textarea */}
      <div className="relative">
        <textarea value={prompt.startsWith(goalData.prompts[0]) || goalData.prompts.includes(prompt) ? '' : prompt}
          onChange={(e) => onChange(e.target.value)}
          placeholder="Ou escreva sua própria pergunta..."
          rows={3}
          className="w-full resize-none rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] px-4 py-3 text-[var(--text-sm)] placeholder:text-[var(--muted-foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)] transition-shadow" />
      </div>
    </motion.div>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

export default function WelcomePage() {
  const { isAuthenticated, isLoading, fetchUser, user } = useAuthStore();
  const { setSelectedModel, createConversation } = useChatStore();
  const router = useRouter();

  const [step, setStep] = useState(0);
  const [goal, setGoal] = useState<Goal | null>(null);
  const [modelId, setModelId] = useState<string | null>(null);
  const [firstPrompt, setFirstPrompt] = useState('');

  // Auth guard
  useEffect(() => { fetchUser(); }, [fetchUser]);
  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.replace('/login');
  }, [isLoading, isAuthenticated, router]);

  // If already onboarded, skip to dashboard
  useEffect(() => {
    if (typeof window !== 'undefined' && localStorage.getItem(ONBOARDING_KEY)) {
      router.replace('/');
    }
  }, [router]);

  useEffect(() => {
    trackEvent('onboarding_start');
  }, []);

  const selectedGoalData = goals.find((g) => g.id === goal);
  const recommendedModel = selectedGoalData?.suggestedModel ?? 'gpt-4o-mini';
  const effectiveModel = modelId ?? recommendedModel;

  const canAdvance = [
    goal !== null,        // step 0
    true,                 // step 1 always allowed (has recommended)
    true,                 // step 2 always allowed (can skip prompt)
  ];

  const handleNext = () => {
    if (step < TOTAL_STEPS - 1) {
      setStep((s) => s + 1);
    } else {
      handleFinish();
    }
  };

  const handleFinish = () => {
    // Apply selections
    setSelectedModel(effectiveModel);

    trackEvent('onboarding_complete', {
      goal: goal ?? 'general',
      model: effectiveModel,
      hasPrompt: firstPrompt.trim().length > 0,
    });

    // Mark onboarding done
    if (typeof window !== 'undefined') {
      localStorage.setItem(ONBOARDING_KEY, '1');
    }

    // Navigate to chat with optional first prompt
    const prompt = firstPrompt.trim();
    if (prompt) {
      createConversation();
      router.push(`/chat?prompt=${encodeURIComponent(prompt)}`);
    } else {
      router.push('/chat');
    }
  };

  const handleSkip = () => {
    trackEvent('onboarding_skip', { step });
    if (typeof window !== 'undefined') localStorage.setItem(ONBOARDING_KEY, '1');
    router.push('/chat');
  };

  if (isLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <div className="flex gap-1">
          <span className="pulse-dot" /><span className="pulse-dot" /><span className="pulse-dot" />
        </div>
      </main>
    );
  }

  if (!isAuthenticated) return null;

  const stepLabels = ['Objetivo', 'Modelo', 'Primeiro prompt'];

  return (
    <main className="relative min-h-screen bg-[var(--background)] flex items-center justify-center overflow-hidden px-4 py-12">
      {/* Background glow */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden>
        <div className="absolute -top-40 left-1/2 h-[600px] w-[600px] -translate-x-1/2 rounded-full bg-[var(--brand-primary)]/6 blur-[80px]" />
        <div className="absolute bottom-0 right-0 h-[400px] w-[400px] translate-x-1/4 translate-y-1/4 rounded-full bg-[var(--brand-secondary)]/5 blur-[64px]" />
      </div>

      <div className="relative w-full max-w-xl">
        {/* Logo */}
        <motion.div initial={{ opacity: 0, y: -12 }} animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="mb-8 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span className="inline-flex h-9 w-9 items-center justify-center rounded-[var(--radius-lg)] text-white shadow-[var(--shadow-brand)]"
              style={{ background: 'var(--brand-gradient)' }}>
              <Bot className="h-5 w-5" />
            </span>
            <span className="text-[var(--text-base)] font-bold gradient-text">IA Aggregator</span>
          </div>
          <button type="button" onClick={handleSkip}
            className="text-[var(--text-xs)] text-[var(--muted-foreground)] hover:text-[var(--foreground)] transition-colors">
            Pular configuração →
          </button>
        </motion.div>

        {/* Progress */}
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.1 }}
          className="mb-6 flex items-center justify-between">
          <StepDots current={step} total={TOTAL_STEPS} />
          <span className="text-[var(--text-xs)] text-[var(--muted-foreground)]">
            {stepLabels[step]} · {step + 1} de {TOTAL_STEPS}
          </span>
        </motion.div>

        {/* Welcome banner on step 0 */}
        {step === 0 && (
          <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 }}
            className="mb-5 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] px-5 py-4 flex items-center gap-3">
            <div className="shrink-0 rounded-full bg-[var(--brand-primary)]/10 p-2.5">
              <Sparkles className="h-5 w-5 text-[var(--brand-primary)]" />
            </div>
            <div>
              <p className="text-[var(--text-sm)] font-semibold">
                Bem-vindo, {user?.fullName?.split(' ')[0] || 'usuário'}! 👋
              </p>
              <p className="text-[var(--text-xs)] text-[var(--muted-foreground)] mt-0.5">
                Leva menos de 1 minuto para configurar. Vamos lá.
              </p>
            </div>
          </motion.div>
        )}

        {/* Card */}
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface-1)] shadow-[var(--shadow-xl)] overflow-hidden">
          <div className="p-6 md:p-7">
            <AnimatePresence mode="wait">
              {step === 0 && (
                <StepGoal key="goal" selected={goal} onSelect={(g) => { setGoal(g); }} />
              )}
              {step === 1 && (
                <StepModel key="model" selected={modelId} recommended={recommendedModel} onSelect={setModelId} />
              )}
              {step === 2 && goal && (
                <StepPrompt key="prompt" goal={goal} prompt={firstPrompt} onChange={setFirstPrompt} />
              )}
            </AnimatePresence>
          </div>

          {/* Footer / CTA */}
          <div className="flex items-center justify-between border-t border-[var(--border)] bg-[var(--surface-2)] px-6 py-4">
            {step > 0 ? (
              <button type="button" onClick={() => setStep((s) => s - 1)}
                className="text-[var(--text-sm)] text-[var(--muted-foreground)] hover:text-[var(--foreground)] transition-colors">
                ← Voltar
              </button>
            ) : (
              <span />
            )}
            <motion.button type="button" onClick={handleNext}
              disabled={!canAdvance[step]}
              whileTap={{ scale: 0.97 }}
              style={{ background: 'var(--brand-gradient)' }}
              className="flex items-center gap-2 rounded-[var(--radius-lg)] px-6 py-2.5 text-[var(--text-sm)] font-semibold text-white shadow-[var(--shadow-brand)] transition-all hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40">
              {step < TOTAL_STEPS - 1 ? (
                <>Continuar <ChevronRight className="h-4 w-4" /></>
              ) : (
                <>Começar agora <ArrowRight className="h-4 w-4" /></>
              )}
            </motion.button>
          </div>
        </motion.div>

        {/* Step summary (model selected) */}
        {step > 0 && goal && (
          <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }}
            className="mt-4 flex items-center gap-2 pl-1">
            {(() => {
              const g = goals.find((x) => x.id === goal);
              if (!g) return null;
              const Icon = g.icon;
              return (
                <span className={cn('inline-flex items-center gap-1.5 rounded-full border border-[var(--border)] bg-[var(--surface-1)] px-2.5 py-1 text-[10px]', g.color)}>
                  <Icon className="h-3 w-3" /> {g.label}
                </span>
              );
            })()}
            {step > 1 && (
              <span className="inline-flex items-center gap-1.5 rounded-full border border-[var(--border)] bg-[var(--surface-1)] px-2.5 py-1 text-[10px] text-[var(--muted-foreground)]">
                {models.find((m) => m.id === effectiveModel)?.label ?? effectiveModel}
              </span>
            )}
          </motion.div>
        )}
      </div>
    </main>
  );
}
