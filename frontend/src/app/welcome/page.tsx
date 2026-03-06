'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AnimatePresence, motion } from 'framer-motion';
import {
  ArrowRight,
  BarChart3,
  Bot,
  Brain,
  Check,
  ChevronLeft,
  ChevronRight,
  FileText,
  MessageSquare,
  Scale,
  Sparkles,
  Target,
  Zap,
} from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/cn';
import { trackEvent } from '@/lib/analytics';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';

type Goal = 'analysis' | 'writing' | 'planning' | 'general';
type Tier = 'fast' | 'balanced' | 'powerful';

type GoalCard = {
  id: Goal;
  title: string;
  description: string;
  icon: React.ElementType;
  colorClass: string;
  suggestedModel: string;
  prompts: string[];
};

type ModelCard = {
  id: string;
  label: string;
  provider: string;
  tier: Tier;
  color: string;
};

const ONBOARDING_KEY = 'ia-onboarding-done';
const TOTAL_STEPS = 3;

const goals: GoalCard[] = [
  {
    id: 'analysis',
    title: 'Analise e relatorios',
    description: 'Entenda dados, sintetize contexto e gere respostas executivas com clareza.',
    icon: BarChart3,
    colorClass: 'text-[var(--brand-primary)]',
    suggestedModel: 'gpt-4.1-mini',
    prompts: [
      'Crie um resumo executivo em 5 pontos sobre:',
      'Analise estes dados e destaque riscos, oportunidades e proximos passos:',
      'Compare os cenarios abaixo e recomende a melhor direcao:',
    ],
  },
  {
    id: 'writing',
    title: 'Escrita e comúnicação',
    description: 'Emails, documentos, comúnicados e textos com mais consistencia e velocidade.',
    icon: FileText,
    colorClass: 'text-[var(--brand-secondary)]',
    suggestedModel: 'claude-3-5-haiku',
    prompts: [
      'Escreva um email profissional e objetivo sobre:',
      'Reescreva este texto para deixá-lo mais claro e persuasivo:',
      'Crie um comúnicado interno com tom executivo para:',
    ],
  },
  {
    id: 'planning',
    title: 'Planejamento e projetos',
    description: 'Quebre metas em planos, cronogramas, prioridades e entregas acionaveis.',
    icon: Target,
    colorClass: 'text-[var(--success)]',
    suggestedModel: 'gpt-4o-mini',
    prompts: [
      'Monte um plano de ação com etapas, responsaveis e riscos para:',
      'Crie um roadmap de 90 dias para atingir este objetivo:',
      'Transforme o objetivo abaixo em milestones semanais:',
    ],
  },
  {
    id: 'general',
    title: 'Assistente geral',
    description: 'Pesquise, explore, aprenda e teste ideias sem travar em uma única categoria.',
    icon: Sparkles,
    colorClass: 'text-[var(--warning)]',
    suggestedModel: 'deepseek-chat',
    prompts: [
      'Explique este conceito com exemplos praticos:',
      'Quais sao as melhores abordagens para resolver:',
      'Crie um plano inicial para explorar esta oportunidade:',
    ],
  },
];

const models: ModelCard[] = [
  { id: 'gpt-4o-mini', label: 'GPT-4o mini', provider: 'OpenAI', tier: 'fast', color: '#4ed9a7' },
  { id: 'gpt-4.1-mini', label: 'GPT-4.1 mini', provider: 'OpenAI', tier: 'balanced', color: '#6073ff' },
  { id: 'claude-3-5-haiku', label: 'Claude Haiku', provider: 'Anthropic', tier: 'fast', color: '#f25d9c' },
  { id: 'gemini-1.5-flash', label: 'Gemini Flash', provider: 'Google', tier: 'fast', color: '#77b8ff' },
  { id: 'deepseek-chat', label: 'DeepSeek Chat', provider: 'DeepSeek', tier: 'balanced', color: '#8b6cff' },
  { id: 'command-r-plus', label: 'Command R+', provider: 'Cohere', tier: 'powerful', color: '#ffbf66' },
];

const tierMeta: Record<Tier, { label: string; icon: React.ElementType; tone: string }> = {
  fast: { label: 'Rápido', icon: Zap, tone: 'text-[var(--success)]' },
  balanced: { label: 'Equilibrado', icon: Scale, tone: 'text-[var(--brand-primary)]' },
  powerful: { label: 'Profundo', icon: Brain, tone: 'text-[var(--warning)]' },
};

function StepIndicator({ current }: { current: number }) {
  return (
    <div className="flex items-center gap-2">
      {Array.from({ length: TOTAL_STEPS }, (_, index) => (
        <motion.span
          key={index}
          animate={{ width: index === current ? 36 : 10, opacity: index <= current ? 1 : 0.55 }}
          className={cn('h-2 rounded-full', index <= current ? 'bg-[var(--brand-primary)]' : 'bg-[var(--surface-4)]')}
        />
      ))}
    </div>
  );
}

export default function WelcomePage() {
  const { isAuthenticated, isLoading, user } = useAuthStore();
  const { setSelectedModel, createConversation } = useChatStore();
  const router = useRouter();

  const [step, setStep] = useState(0);
  const [goal, setGoal] = useState<Goal | null>(null);
  const [modelId, setModelId] = useState<string | null>(null);
  const [firstPrompt, setFirstPrompt] = useState('');

  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.replace('/login');
  }, [isAuthenticated, isLoading, router]);

  useEffect(() => {
    if (typeof window !== 'undefined' && localStorage.getItem(ONBOARDING_KEY)) {
      router.replace('/');
    }
  }, [router]);

  useEffect(() => {
    trackEvent('onboarding_start');
  }, []);

  const selectedGoal = useMemo(() => goals.find((item) => item.id === goal) ?? goals[0], [goal]);
  const effectiveModel = modelId ?? selectedGoal.suggestedModel;
  const stepLabels = ['Objetivo', 'Modelo', 'Primeiro prompt'];

  const handleFinish = () => {
    setSelectedModel(effectiveModel);
    trackEvent('onboarding_complete', {
      goal: goal ?? 'general',
      model: effectiveModel,
      hasPrompt: Boolean(firstPrompt.trim()),
    });

    if (typeof window !== 'undefined') {
      localStorage.setItem(ONBOARDING_KEY, '1');
    }

    if (firstPrompt.trim()) {
      createConversation();
      router.push(`/chat?prompt=${encodeURIComponent(firstPrompt.trim())}`);
      return;
    }

    router.push('/chat');
  };

  const handleSkip = () => {
    trackEvent('onboarding_skip', { step });
    if (typeof window !== 'undefined') {
      localStorage.setItem(ONBOARDING_KEY, '1');
    }
    router.push('/chat');
  };

  if (isLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <div className="flex gap-1">
          <span className="pulse-dot" />
          <span className="pulse-dot" />
          <span className="pulse-dot" />
        </div>
      </main>
    );
  }

  if (!isAuthenticated) return null;

  return (
    <main className="relative min-h-screen overflow-hidden px-4 py-10">
      <div className="pointer-events-none absolute inset-0" aria-hidden>
        <div className="absolute left-[8%] top-[-10%] h-[24rem] w-[24rem] rounded-full bg-[rgba(96,115,255,0.18)] blur-[120px]" />
        <div className="absolute bottom-[-12%] right-[6%] h-[24rem] w-[24rem] rounded-full bg-[rgba(242,93,156,0.14)] blur-[120px]" />
      </div>

      <div className="relative lume-section max-w-3xl">
        <motion.div initial={{ opacity: 0, y: -12 }} animate={{ opacity: 1, y: 0 }} className="mb-6 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="inline-flex h-12 w-12 items-center justify-center rounded-[18px] text-white shadow-[var(--shadow-brand)]" style={{ background: 'var(--brand-gradient)' }}>
              <Bot className="h-5 w-5" />
            </span>
            <div>
              <p className="text-[1rem] font-semibold tracking-[-0.05em] text-[var(--foreground)]">Lume</p>
              <p className="text-[0.68rem] uppercase tracking-[0.2em] text-[var(--subtle-foreground)]">Onboarding premium</p>
            </div>
          </div>
          <button type="button" onClick={handleSkip} className="text-[0.82rem] font-medium text-[var(--muted-foreground)] hover:text-[var(--foreground)]">
            Pular e ir para o chat
          </button>
        </motion.div>

        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mb-5 flex items-center justify-between gap-4">
          <StepIndicator current={step} />
          <span className="text-[0.78rem] font-medium text-[var(--muted-foreground)]">{stepLabels[step]} · {step + 1} de {TOTAL_STEPS}</span>
        </motion.div>

        {step === 0 && (
          <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} className="lume-panel mb-5 flex items-center gap-3 rounded-[var(--radius-xl)] px-5 py-4">
            <span className="inline-flex h-11 w-11 items-center justify-center rounded-full bg-[rgba(96,115,255,0.12)] text-[var(--brand-primary)]">
              <Sparkles className="h-5 w-5" />
            </span>
            <div>
              <p className="text-[var(--text-sm)] font-semibold text-[var(--foreground)]">Bem-vindo, {user?.fullName?.split(' ')[0] || 'usuario'}.</p>
              <p className="mt-1 text-[0.82rem] text-[var(--muted-foreground)]">Em menos de um minuto o Lume ajusta o fluxo inicial para o seu estilo de uso.</p>
            </div>
          </motion.div>
        )}

        <motion.section initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="lume-panel overflow-hidden rounded-[var(--radius-2xl)]">
          <div className="p-6 md:p-8">
            <AnimatePresence mode="wait">
              {step === 0 && (
                <motion.div key="goal" initial={{ opacity: 0, x: 24 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -24 }} className="space-y-5">
                  <div>
                    <Badge variant="brand">Passo 1</Badge>
                    <h1 className="mt-4 text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">Qual e o seu foco principal?</h1>
                    <p className="mt-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">A resposta ajuda a sugerir o melhor modelo, os primeiros templates e a configuracao do workspace.</p>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {goals.map((item) => {
                      const Icon = item.icon;
                      const active = goal === item.id;
                      return (
                        <button
                          key={item.id}
                          type="button"
                          onClick={() => setGoal(item.id)}
                          className={cn(
                            'rounded-[var(--radius-xl)] border p-5 text-left transition-all',
                            active
                              ? 'border-[rgba(96,115,255,0.28)] bg-[rgba(96,115,255,0.1)] shadow-[0_0_0_1px_rgba(96,115,255,0.28)]'
                              : 'border-[var(--border)] bg-[rgba(255,255,255,0.03)] hover:border-[var(--border-strong)] hover:bg-[rgba(255,255,255,0.05)]'
                          )}
                        >
                          <span className="flex items-start justify-between gap-3">
                            <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] bg-[rgba(255,255,255,0.04)]">
                              <Icon className={cn('h-5 w-5', item.colorClass)} />
                            </span>
                            {active && <Check className="h-5 w-5 text-[var(--brand-primary)]" />}
                          </span>
                          <h2 className="mt-4 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">{item.title}</h2>
                          <p className="mt-2 text-[0.84rem] leading-7 text-[var(--muted-foreground)]">{item.description}</p>
                        </button>
                      );
                    })}
                  </div>
                </motion.div>
              )}

              {step === 1 && (
                <motion.div key="model" initial={{ opacity: 0, x: 24 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -24 }} className="space-y-5">
                  <div>
                    <Badge variant="brand">Passo 2</Badge>
                    <h1 className="mt-4 text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">Escolha o modelo que vai guiar o inicio.</h1>
                    <p className="mt-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">Voce pode trocar a qualquer momento no chat. O Lume ja sugere um modelo com base no objetivo escolhido.</p>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                    {models.map((model) => {
                      const tier = tierMeta[model.tier];
                      const TierIcon = tier.icon;
                      const active = effectiveModel === model.id;
                      return (
                        <button
                          key={model.id}
                          type="button"
                          onClick={() => setModelId(model.id)}
                          className={cn(
                            'rounded-[var(--radius-xl)] border p-5 text-left transition-all',
                            active
                              ? 'border-[rgba(96,115,255,0.28)] bg-[rgba(96,115,255,0.1)] shadow-[0_0_0_1px_rgba(96,115,255,0.28)]'
                              : 'border-[var(--border)] bg-[rgba(255,255,255,0.03)] hover:border-[var(--border-strong)] hover:bg-[rgba(255,255,255,0.05)]'
                          )}
                        >
                          <span className="flex items-start justify-between gap-3">
                            <span className="h-3 w-3 rounded-full" style={{ backgroundColor: model.color }} />
                            {active && <Check className="h-5 w-5 text-[var(--brand-primary)]" />}
                          </span>
                          <p className="mt-4 text-[var(--text-base)] font-semibold text-[var(--foreground)]">{model.label}</p>
                          <p className="mt-1 text-[0.78rem] text-[var(--muted-foreground)]">{model.provider}</p>
                          <span className={cn('mt-3 inline-flex items-center gap-1 text-[0.72rem] font-medium', tier.tone)}>
                            <TierIcon className="h-3.5 w-3.5" /> {tier.label}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </motion.div>
              )}

              {step === 2 && (
                <motion.div key="prompt" initial={{ opacity: 0, x: 24 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -24 }} className="space-y-5">
                  <div>
                    <Badge variant="brand">Passo 3</Badge>
                    <h1 className="mt-4 text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">Escolha o primeiro prompt ou escreva o seu.</h1>
                    <p className="mt-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">Voce ja entra no chat com contexto e direcao. O objetivo aqui e eliminar a tela vazia.</p>
                  </div>
                  <div className="space-y-3">
                    {selectedGoal.prompts.map((prompt) => (
                      <button
                        key={prompt}
                        type="button"
                        onClick={() => setFirstPrompt(prompt)}
                        className={cn(
                          'flex w-full items-start gap-3 rounded-[var(--radius-xl)] border px-4 py-4 text-left transition-all',
                          firstPrompt === prompt
                            ? 'border-[rgba(96,115,255,0.28)] bg-[rgba(96,115,255,0.1)]'
                            : 'border-[var(--border)] bg-[rgba(255,255,255,0.03)] hover:border-[var(--border-strong)] hover:bg-[rgba(255,255,255,0.05)]'
                        )}
                      >
                        <MessageSquare className="mt-0.5 h-4 w-4 shrink-0 text-[var(--brand-primary)]" />
                        <span className="text-[var(--text-sm)] leading-7 text-[var(--foreground)]">{prompt}</span>
                      </button>
                    ))}
                  </div>
                  <textarea
                    value={selectedGoal.prompts.includes(firstPrompt) ? '' : firstPrompt}
                    onChange={(event) => setFirstPrompt(event.target.value)}
                    rows={4}
                    placeholder="Ou escreva sua propria pergunta inicial..."
                    className="w-full resize-none rounded-[var(--radius-xl)] border border-[var(--input)] bg-[rgba(9,17,31,0.68)] px-4 py-4 text-[var(--text-sm)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus:outline-none focus:shadow-[0_0_0_4px_rgba(96,115,255,0.12)]"
                  />
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <div className="flex items-center justify-between border-t border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-6 py-4">
            {step > 0 ? (
              <button type="button" onClick={() => setStep((current) => current - 1)} className="inline-flex items-center gap-2 text-[0.84rem] font-medium text-[var(--muted-foreground)] hover:text-[var(--foreground)]">
                <ChevronLeft className="h-4 w-4" /> Voltar
              </button>
            ) : <span />}

            {step < TOTAL_STEPS - 1 ? (
              <Button variant="brand" size="lg" onClick={() => setStep((current) => current + 1)} disabled={step === 0 && !goal}>
                Continuar <ChevronRight className="h-4 w-4" />
              </Button>
            ) : (
              <Button variant="brand" size="lg" onClick={handleFinish}>
                Entrar no chat <ArrowRight className="h-4 w-4" />
              </Button>
            )}
          </div>
        </motion.section>
      </div>
    </main>
  );
}

