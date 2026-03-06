'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  ArrowUp,
  Bot,
  Brain,
  Building2,
  CreditCard,
  Layers,
  Lock,
  MessageSquare,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  WandSparkles,
  Zap,
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useAuthStore } from '@/stores/auth-store';

const heroPrompts = [
  'Resumo executivo',
  'Plano de acao',
  'Email profissional',
  'Analise comparativa',
];

const trustMetrics = [
  { value: '13+', label: 'Modelos premium no mesmo painel' },
  { value: '1', label: 'Workspace para chat, templates, acervo e insights' },
  { value: '24/7', label: 'Operacao pronta para desktop e mobile' },
  { value: '0', label: 'Atrito entre descoberta, execucao e memoria' },
];

const modelStrip = ['GPT', 'Claude', 'Gemini', 'DeepSeek', 'Llama', 'Mistral', 'Perplexity', 'Grok'];

const capabilityCards = [
  {
    title: 'Chat multimodelo com decisao mais rapida',
    description: 'Troque de modelo sem trocar de fluxo. Converse, refine, compare respostas e mantenha o historico limpo.',
    icon: MessageSquare,
  },
  {
    title: 'Templates prontos para times reais',
    description: 'Use prompts curados para planejamento, vendas, marketing, analise, comunicacao e operacao diaria.',
    icon: WandSparkles,
  },
  {
    title: 'Biblioteca operacional, nao deposito morto',
    description: 'Recupere conversas, reutilize conhecimento e transforme interacoes em ativos reutilizaveis.',
    icon: Layers,
  },
  {
    title: 'Insights para evoluir adocao de IA',
    description: 'Entenda onboarding, funil, retencao e ritmo de uso para melhorar o impacto do produto.',
    icon: TrendingUp,
  },
];

const workflowBlocks = [
  {
    step: '01',
    title: 'Comece por um objetivo claro',
    body: 'Escrita, analise, planejamento ou exploracao geral. O sistema ja puxa o melhor ponto de partida.',
  },
  {
    step: '02',
    title: 'Acelere com modelo e template certos',
    body: 'Selecione o motor ideal e reaproveite estruturas de prompt desenhadas para contexto de negocio.',
  },
  {
    step: '03',
    title: 'Transforme conversa em operacao',
    body: 'Salve, recupere, compare e organize resultados dentro da biblioteca e das paginas do app.',
  },
];

const securityPoints = [
  'Workspace com navegacao consistente e dados organizados por fluxo.',
  'Interface pensada para times que usam IA todos os dias, nao apenas para testes.',
  'Camada visual premium com contraste, hierarquia e CTA claros em todas as rotas.',
];

function LandingPage({ isAuthenticated }: { isAuthenticated: boolean }) {
  const router = useRouter();
  const [promptText, setPromptText] = useState('');

  const heroCta = useMemo(() => ({
    href: isAuthenticated ? '/home' : '/register',
    label: isAuthenticated ? 'Abrir workspace' : 'Comecar agora',
  }), [isAuthenticated]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = promptText.trim();
    if (!trimmed) return;
    router.push(`/chat?prompt=${encodeURIComponent(trimmed)}`);
  };

  return (
    <main id="main-content" className="pb-16 pt-3 md:pt-4">
      <header className="lume-section sticky top-3 z-40 rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(8,17,31,0.76)] px-4 py-3 shadow-[var(--shadow-md)] backdrop-blur-md md:px-5">
        <div className="flex items-center justify-between gap-4">
          <Link href="/" className="inline-flex items-center gap-3 text-[var(--text-sm)] font-semibold text-[var(--foreground)]">
            <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] text-white shadow-[var(--shadow-brand)]" style={{ background: 'var(--brand-gradient)' }}>
              <Bot className="h-5 w-5" />
            </span>
            <span className="min-w-0">
              <span className="block text-[1rem] tracking-[-0.05em]">Lume</span>
              <span className="block text-[0.64rem] uppercase tracking-[0.2em] text-[var(--subtle-foreground)]">AI Workspace</span>
            </span>
          </Link>

          <nav className="hidden items-center gap-6 text-[0.84rem] font-medium text-[var(--muted-foreground)] md:flex">
            <Link href="/library" className="hover:text-[var(--foreground)]">Biblioteca</Link>
            <Link href="/prompts" className="hover:text-[var(--foreground)]">Templates</Link>
            <Link href="/billing" className="hover:text-[var(--foreground)]">Plano</Link>
          </nav>

          <div className="flex items-center gap-2">
            {isAuthenticated ? (
              <Link href="/chat" className="hidden rounded-[var(--radius-pill)] border border-[var(--border)] px-4 py-2 text-[0.82rem] font-semibold text-[var(--foreground)] hover:border-[var(--border-strong)] md:inline-flex">
                Ir para chat
              </Link>
            ) : (
              <Link href="/login" className="hidden rounded-[var(--radius-pill)] border border-[var(--border)] px-4 py-2 text-[0.82rem] font-semibold text-[var(--foreground)] hover:border-[var(--border-strong)] md:inline-flex">
                Entrar
              </Link>
            )}
            <Button variant="brand" size="md" onClick={() => router.push(heroCta.href)}>
              {heroCta.label}
            </Button>
          </div>
        </div>
      </header>

      <section className="lume-section pt-14 md:pt-20">
        <div className="grid gap-10 lg:grid-cols-[1.05fr_0.95fr] lg:items-center">
          <div>
            <motion.div initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.35 }}>
              <span className="lume-kicker">
                <Sparkles className="h-3.5 w-3.5" />
                Lume unifica os fluxos mais importantes de IA para negocio
              </span>
              <h1 className="lume-display mt-5 text-[var(--foreground)]">
                Execute com IA em um workspace premium, consistente e pronto para escalar.
              </h1>
              <p className="lume-subtitle mt-5">
                Chat multimodelo, templates curados, biblioteca operacional e analytics no mesmo lugar, com experiencia visual limpa e foco total em produtividade.
              </p>
            </motion.div>

            <motion.form
              onSubmit={handleSubmit}
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.35, delay: 0.05 }}
              className="lume-panel mt-8 rounded-[var(--radius-2xl)] p-4 md:p-5"
            >
              <div className="flex min-h-[5.25rem] items-end gap-3 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-3">
                <textarea
                  value={promptText}
                  onChange={(event) => setPromptText(event.target.value)}
                  rows={2}
                  placeholder="O que voce quer destravar hoje?"
                  className="flex-1 resize-none bg-transparent text-[var(--text-sm)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus:outline-none"
                  style={{ maxHeight: '160px' }}
                />
                <button
                  type="submit"
                  aria-label="Enviar prompt"
                  className="mb-1 inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-white shadow-[var(--shadow-brand)] disabled:opacity-40"
                  style={{ background: 'var(--brand-gradient)' }}
                  disabled={!promptText.trim()}
                >
                  <ArrowUp className="h-4 w-4" />
                </button>
              </div>
              <div className="mt-3 flex flex-wrap gap-2">
                {heroPrompts.map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => setPromptText(item)}
                    className="rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(255,255,255,0.04)] px-3 py-2 text-[0.76rem] font-medium text-[var(--muted-foreground)] hover:border-[var(--border-strong)] hover:text-[var(--foreground)]"
                  >
                    {item}
                  </button>
                ))}
              </div>
            </motion.form>

            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.15 }} className="mt-8 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {trustMetrics.map((item) => (
                <div key={item.label} className="lume-panel rounded-[var(--radius-xl)] px-4 py-4">
                  <p className="text-[var(--text-2xl)] font-semibold text-[var(--foreground)]">{item.value}</p>
                  <p className="mt-1 text-[0.78rem] leading-6 text-[var(--muted-foreground)]">{item.label}</p>
                </div>
              ))}
            </motion.div>
          </div>

          <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.4, delay: 0.08 }}>
            <div className="lume-panel rounded-[var(--radius-2xl)] p-5 md:p-6">
              <div className="flex items-center justify-between gap-3 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-3">
                <div>
                  <p className="text-[0.68rem] uppercase tracking-[0.2em] text-[var(--subtle-foreground)]">Workspace snapshot</p>
                  <p className="mt-1 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Tudo conectado em uma unica experiencia</p>
                </div>
                <Badge variant="brand">Live</Badge>
              </div>

              <div className="mt-4 grid gap-4 md:grid-cols-[0.92fr_1.08fr]">
                <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
                  <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Modelos</p>
                  <div className="mt-4 grid grid-cols-2 gap-2">
                    {modelStrip.map((model) => (
                      <div key={model} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-3 text-[0.82rem] font-medium text-[var(--foreground)]">
                        {model}
                      </div>
                    ))}
                  </div>
                </div>
                <div className="rounded-[var(--radius-xl)] border border-[rgba(96,115,255,0.22)] bg-[var(--brand-gradient-soft)] p-4">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Fluxo principal</p>
                      <p className="mt-1 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Da pergunta ao ativo de negocio</p>
                    </div>
                    <Brain className="h-5 w-5 text-[var(--brand-primary)]" />
                  </div>
                  <div className="mt-4 space-y-3">
                    {['Chat multimodelo', 'Template aplicado', 'Historico salvo', 'Insights atualizados'].map((item, index) => (
                      <div key={item} className="flex items-center gap-3 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(8,17,31,0.62)] px-4 py-3">
                        <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-[rgba(96,115,255,0.14)] text-[0.72rem] font-semibold text-[var(--brand-primary)]">0{index + 1}</span>
                        <p className="text-[0.84rem] text-[var(--foreground)]">{item}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      <section className="lume-section mt-12 md:mt-16">
        <div className="grid gap-5 lg:grid-cols-[0.94fr_1.06fr]">
          <div className="lume-panel rounded-[var(--radius-2xl)] p-6 md:p-7">
            <Badge variant="outline">Por que Lume</Badge>
            <h2 className="mt-4 text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">Menos ferramentas isoladas. Mais operacao com contexto continuo.</h2>
            <p className="mt-4 text-[var(--text-sm)] leading-7 text-[var(--muted-foreground)]">
              O Lume foi desenhado para distribuir proposta de valor, prova de capacidade e CTA em blocos claros. Resultado: menos friccao e mais execucao.
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
                <p className="text-[0.7rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Operacao solta</p>
                <p className="mt-2 text-[2rem] font-semibold text-[var(--foreground)]">5+</p>
                <p className="mt-2 text-[0.82rem] text-[var(--muted-foreground)]">Ferramentas, temas e estilos desconectados.</p>
              </div>
              <div className="rounded-[var(--radius-xl)] border border-[rgba(96,115,255,0.22)] bg-[var(--brand-gradient-soft)] p-4">
                <p className="text-[0.7rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Operacao Lume</p>
                <p className="mt-2 text-[2rem] font-semibold text-[var(--foreground)]">1</p>
                <p className="mt-2 text-[0.82rem] text-[var(--muted-foreground)]">Sistema coerente para descoberta, execucao e gestao.</p>
              </div>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            {capabilityCards.map((card) => (
              <div key={card.title} className="lume-panel rounded-[var(--radius-2xl)] p-5 md:p-6">
                <span className="inline-flex h-12 w-12 items-center justify-center rounded-[18px] bg-[rgba(96,115,255,0.12)] text-[var(--brand-primary)]">
                  <card.icon className="h-5 w-5" />
                </span>
                <h3 className="mt-4 text-[var(--text-xl)] font-semibold text-[var(--foreground)]">{card.title}</h3>
                <p className="mt-3 text-[var(--text-sm)] leading-7 text-[var(--muted-foreground)]">{card.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="lume-section mt-12 md:mt-16">
        <div className="lume-panel rounded-[var(--radius-2xl)] p-6 md:p-8">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <Badge variant="brand">Workflow</Badge>
              <h2 className="mt-4 text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">Um caminho limpo entre intencao, resposta e memoria.</h2>
            </div>
            <Button variant="outline" size="md" onClick={() => router.push(heroCta.href)}>
              Ver na pratica
            </Button>
          </div>
          <div className="mt-6 grid gap-4 lg:grid-cols-3">
            {workflowBlocks.map((item) => (
              <div key={item.step} className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5">
                <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Etapa {item.step}</p>
                <h3 className="mt-3 text-[var(--text-xl)] font-semibold text-[var(--foreground)]">{item.title}</h3>
                <p className="mt-3 text-[var(--text-sm)] leading-7 text-[var(--muted-foreground)]">{item.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="lume-section mt-12 md:mt-16">
        <div className="grid gap-5 lg:grid-cols-[0.92fr_1.08fr]">
          <div className="lume-panel rounded-[var(--radius-2xl)] p-6 md:p-8">
            <Badge variant="outline">Seguranca e confianca</Badge>
            <h2 className="mt-4 text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">Uma interface escura, precisa e madura para uso recorrente.</h2>
            <div className="mt-6 space-y-3">
              {securityPoints.map((point) => (
                <div key={point} className="flex items-start gap-3 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-4">
                  <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-[var(--brand-primary)]" />
                  <p className="text-[var(--text-sm)] leading-7 text-[var(--muted-foreground)]">{point}</p>
                </div>
              ))}
            </div>
          </div>
          <div className="lume-panel rounded-[var(--radius-2xl)] p-6 md:p-8">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5">
                <Building2 className="h-5 w-5 text-[var(--brand-primary)]" />
                <h3 className="mt-3 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Workspace para times</h3>
                <p className="mt-2 text-[var(--text-sm)] leading-7 text-[var(--muted-foreground)]">Landing, auth e area autenticada com assinatura visual unica.</p>
              </div>
              <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5">
                <CreditCard className="h-5 w-5 text-[var(--brand-primary)]" />
                <h3 className="mt-3 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Valor claro</h3>
                <p className="mt-2 text-[var(--text-sm)] leading-7 text-[var(--muted-foreground)]">Plano, uso e consumo com leitura executiva e CTA objetivo.</p>
              </div>
              <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5">
                <Lock className="h-5 w-5 text-[var(--brand-primary)]" />
                <h3 className="mt-3 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Navegacao disciplinada</h3>
                <p className="mt-2 text-[var(--text-sm)] leading-7 text-[var(--muted-foreground)]">Sidebar, menu mobile, header e toasts falam a mesma linguagem.</p>
              </div>
              <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5">
                <Zap className="h-5 w-5 text-[var(--brand-primary)]" />
                <h3 className="mt-3 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Pronto para operar</h3>
                <p className="mt-2 text-[var(--text-sm)] leading-7 text-[var(--muted-foreground)]">Design orientado a repeticao de uso, nao apenas primeira impressao.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="lume-section mt-12 md:mt-16">
        <div className="rounded-[var(--radius-2xl)] border border-[rgba(96,115,255,0.2)] bg-[var(--brand-gradient-soft)] px-6 py-8 text-center shadow-[var(--shadow-lg)] md:px-10 md:py-12">
          <Badge variant="brand">Lume launch</Badge>
          <h2 className="mt-4 text-[clamp(2rem,4vw,3.5rem)] font-semibold tracking-[-0.06em] text-[var(--foreground)]">
            Um frontend que finalmente faz jus ao produto.
          </h2>
          <p className="mx-auto mt-4 max-w-2xl text-[var(--text-base)] leading-8 text-[var(--muted-foreground)]">
            Entre, crie sua conta ou leve o primeiro prompt para o chat. O foco e transformar o Lume em referencia visual e operacional.
          </p>
          <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
            <Button variant="brand" size="lg" onClick={() => router.push(heroCta.href)}>
              {isAuthenticated ? 'Abrir workspace' : 'Criar conta'} <ArrowRight className="h-4 w-4" />
            </Button>
            <Button variant="outline" size="lg" onClick={() => router.push(isAuthenticated ? '/chat' : '/login')}>
              {isAuthenticated ? 'Ir para chat' : 'Ja tenho acesso'}
            </Button>
          </div>
        </div>
      </section>
    </main>
  );
}

export default function HomePage() {
  const { isAuthenticated, isLoading } = useAuthStore();

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

  return <LandingPage isAuthenticated={isAuthenticated} />;
}

