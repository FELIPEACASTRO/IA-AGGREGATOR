'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  ArrowRight,
  BarChart3,
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
    suggestedModel: 'gpt-4.1-mini',
    prompts: [
      'Crie um resumo executivo em 5 pontos sobre:',
      'Analise estes dados e destaque riscos, oportunidades e proximos passos:',
      'Compare os cenarios abaixo e recomende a melhor direcao:',
    ],
  },
  {
    id: 'writing',
    title: 'Escrita e comunicação',
    description: 'Emails, documentos, comunicados e textos com mais consistencia e velocidade.',
    icon: FileText,
    suggestedModel: 'claude-3-5-haiku',
    prompts: [
      'Escreva um email profissional e objetivo sobre:',
      'Reescreva este texto para deixá-lo mais claro e persuasivo:',
      'Crie um comunicado interno com tom executivo para:',
    ],
  },
  {
    id: 'planning',
    title: 'Planejamento e projetos',
    description: 'Quebre metas em planos, cronogramas, prioridades e entregas acionaveis.',
    icon: Target,
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
  balanced: { label: 'Equilibrado', icon: Scale, tone: 'text-[var(--accent)]' },
  powerful: { label: 'Profundo', icon: Brain, tone: 'text-[var(--warning)]' },
};

function StepIndicator({ current }: { current: number }) {
  return (
    <div className="flex items-center gap-2">
      {Array.from({ length: TOTAL_STEPS }, (_, index) => (
        <span
          key={index}
          className={cn(
            'h-2 rounded-full transition-all',
            index === current ? 'w-9' : 'w-2.5',
            index <= current ? 'bg-[var(--accent)]' : 'bg-[var(--border)]',
          )}
        />
      ))}
    </div>
  );
}

export default function WelcomePage() {
  const { user } = useAuthStore();
  const { setSelectedModel, createConversation } = useChatStore();
  const router = useRouter();

  const [step, setStep] = useState(0);
  const [goal, setGoal] = useState<Goal | null>(null);
  const [modelId, setModelId] = useState<string | null>(null);
  const [firstPrompt, setFirstPrompt] = useState('');

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
      <main className="flex min-h-screen items-center justify-center bg-[var(--background)]">
        <div className="flex gap-1.5">
          <span className="pulse-dot" />
          <span className="pulse-dot" />
          <span className="pulse-dot" />
        </div>
      </main>
    );
  }

  if (!isAuthenticated) return null;

  return (
    <main className="min-h-screen bg-[var(--background)] px-4 py-10">
      <div className="mx-auto max-w-3xl">
        {/* Header */}
        <div className="mb-6 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-[var(--accent)] text-white">
              <Sparkles className="h-4 w-4" />
            </span>
            <div>
              <p className="text-[15px] font-semibold text-[var(--foreground)]">Lume</p>
              <p className="text-[11px] uppercase tracking-widest text-[var(--subtle-foreground)]">Onboarding</p>
            </div>
          </div>
          <button type="button" onClick={handleSkip} className="text-[13px] font-medium text-[var(--muted-foreground)] hover:text-[var(--foreground)]">
            Pular e ir para o chat
          </button>
        </div>

        {/* Step indicator */}
        <div className="mb-5 flex items-center justify-between gap-4">
          <StepIndicator current={step} />
          <span className="text-[12px] font-medium text-[var(--muted-foreground)]">{stepLabels[step]} · {step + 1} de {TOTAL_STEPS}</span>
        </div>

        {/* Welcome banner (step 0 only) */}
        {step === 0 && (
          <div className="mb-5 flex items-center gap-3 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] px-5 py-4">
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-[var(--accent-light)] text-[var(--accent)]">
              <Sparkles className="h-5 w-5" />
            </span>
            <div>
              <p className="text-[14px] font-semibold text-[var(--foreground)]">Bem-vindo, {user?.fullName?.split(' ')[0] || 'usuario'}.</p>
              <p className="mt-1 text-[13px] text-[var(--muted-foreground)]">Em menos de um minuto o Lume ajusta o fluxo inicial para o seu estilo de uso.</p>
            </div>
          </div>
        )}

        {/* Main panel */}
        <div className="overflow-hidden rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface)]">
          <div className="p-6 md:p-8">
            {step === 0 && (
              <div className="space-y-5">
                <div>
                  <Badge variant="accent">Passo 1</Badge>
                  <h1 className="mt-4 text-[24px] font-semibold text-[var(--foreground)]">Qual e o seu foco principal?</h1>
                  <p className="mt-2 text-[14px] text-[var(--muted-foreground)]">A resposta ajuda a sugerir o melhor modelo, os primeiros templates e a configuracao do workspace.</p>
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
                          'rounded-[var(--radius-lg)] border p-5 text-left transition-colors',
                          active
                            ? 'border-[var(--accent)] bg-[var(--accent-light)]'
                            : 'border-[var(--border)] bg-[var(--background)] hover:bg-[var(--surface-hover)]',
                        )}
                      >
                        <span className="flex items-start justify-between gap-3">
                          <span className="inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-md)] bg-[var(--surface-hover)]">
                            <Icon className="h-5 w-5 text-[var(--muted-foreground)]" />
                          </span>
                          {active && <Check className="h-5 w-5 text-[var(--accent)]" />}
                        </span>
                        <h2 className="mt-4 text-[15px] font-semibold text-[var(--foreground)]">{item.title}</h2>
                        <p className="mt-2 text-[13px] leading-relaxed text-[var(--muted-foreground)]">{item.description}</p>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {step === 1 && (
              <div className="space-y-5">
                <div>
                  <Badge variant="accent">Passo 2</Badge>
                  <h1 className="mt-4 text-[24px] font-semibold text-[var(--foreground)]">Escolha o modelo que vai guiar o inicio.</h1>
                  <p className="mt-2 text-[14px] text-[var(--muted-foreground)]">Voce pode trocar a qualquer momento no chat. O Lume ja sugere um modelo com base no objetivo escolhido.</p>
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
                          'rounded-[var(--radius-lg)] border p-5 text-left transition-colors',
                          active
                            ? 'border-[var(--accent)] bg-[var(--accent-light)]'
                            : 'border-[var(--border)] bg-[var(--background)] hover:bg-[var(--surface-hover)]',
                        )}
                      >
                        <span className="flex items-start justify-between gap-3">
                          <span className="h-3 w-3 rounded-full" style={{ backgroundColor: model.color }} />
                          {active && <Check className="h-5 w-5 text-[var(--accent)]" />}
                        </span>
                        <p className="mt-4 text-[14px] font-semibold text-[var(--foreground)]">{model.label}</p>
                        <p className="mt-1 text-[12px] text-[var(--muted-foreground)]">{model.provider}</p>
                        <span className={cn('mt-3 inline-flex items-center gap-1 text-[11px] font-medium', tier.tone)}>
                          <TierIcon className="h-3.5 w-3.5" /> {tier.label}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {step === 2 && (
              <div className="space-y-5">
                <div>
                  <Badge variant="accent">Passo 3</Badge>
                  <h1 className="mt-4 text-[24px] font-semibold text-[var(--foreground)]">Escolha o primeiro prompt ou escreva o seu.</h1>
                  <p className="mt-2 text-[14px] text-[var(--muted-foreground)]">Voce ja entra no chat com contexto e direcao. O objetivo aqui e eliminar a tela vazia.</p>
                </div>
                <div className="space-y-3">
                  {selectedGoal.prompts.map((prompt) => (
                    <button
                      key={prompt}
                      type="button"
                      onClick={() => setFirstPrompt(prompt)}
                      className={cn(
                        'flex w-full items-start gap-3 rounded-[var(--radius-lg)] border px-4 py-4 text-left transition-colors',
                        firstPrompt === prompt
                          ? 'border-[var(--accent)] bg-[var(--accent-light)]'
                          : 'border-[var(--border)] bg-[var(--background)] hover:bg-[var(--surface-hover)]',
                      )}
                    >
                      <MessageSquare className="mt-0.5 h-4 w-4 shrink-0 text-[var(--accent)]" />
                      <span className="text-[14px] leading-relaxed text-[var(--foreground)]">{prompt}</span>
                    </button>
                  ))}
                </div>
                <textarea
                  value={selectedGoal.prompts.includes(firstPrompt) ? '' : firstPrompt}
                  onChange={(event) => setFirstPrompt(event.target.value)}
                  rows={4}
                  placeholder="Ou escreva sua propria pergunta inicial..."
                  className="w-full resize-none rounded-[var(--radius-lg)] border border-[var(--input-border)] bg-[var(--input-bg)] px-4 py-4 text-[14px] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus:outline-none focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--ring)]"
                />
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between border-t border-[var(--border)] bg-[var(--background)] px-6 py-4">
            {step > 0 ? (
              <button type="button" onClick={() => setStep((current) => current - 1)} className="inline-flex items-center gap-2 text-[13px] font-medium text-[var(--muted-foreground)] hover:text-[var(--foreground)]">
                <ChevronLeft className="h-4 w-4" /> Voltar
              </button>
            ) : <span />}

            {step < TOTAL_STEPS - 1 ? (
              <Button variant="primary" size="lg" onClick={() => setStep((current) => current + 1)} disabled={step === 0 && !goal}>
                Continuar <ChevronRight className="h-4 w-4" />
              </Button>
            ) : (
              <Button variant="primary" size="lg" onClick={handleFinish}>
                Entrar no chat <ArrowRight className="h-4 w-4" />
              </Button>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
