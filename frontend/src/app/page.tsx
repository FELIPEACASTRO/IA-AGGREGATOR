'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowRight, ArrowUp, Bot, Layers, MessageSquare,
  WandSparkles, Sparkles, BarChart3, BookOpen,
  Activity, Star, ChevronRight, Brain,
} from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { cn } from '@/lib/cn';

const featureCards = [
  {
    icon: Layers,
    title: 'Multi-modelo',
    desc: 'GPT-4o, Claude, Gemini, Mistral, DeepSeek e muito mais em um único painel.',
    gradient: 'from-[#7C3AED]/20 to-[#06B6D4]/10',
    iconColor: 'text-[var(--brand-primary)]',
  },
  {
    icon: Brain,
    title: 'Canvas Mode',
    desc: 'Reorganize mensagens livremente. Crie quadros de ideias com drag-and-drop.',
    gradient: 'from-[#06B6D4]/20 to-[#7C3AED]/10',
    iconColor: 'text-[var(--brand-secondary)]',
  },
  {
    icon: WandSparkles,
    title: 'Templates prontos',
    desc: 'Biblioteca de prompts curados para análises, e-mails, planos de ação e mais.',
    gradient: 'from-[#10b981]/20 to-[#06B6D4]/10',
    iconColor: 'text-[var(--success)]',
  },
];

const statsData = [
  { label: 'Modelos disponíveis', value: '13+', icon: Layers },
  { label: 'Templates prontos', value: '20+', icon: WandSparkles },
  { label: 'Tokens por mês', value: 'Ilimitado', icon: Activity },
  { label: 'Uptime garantido', value: '99.9%', icon: Star },
];

const quickActions = [
  { label: 'Resumo executivo', prompt: 'Crie um resumo executivo em 5 bullets sobre:' },
  { label: 'Plano de ação', prompt: 'Monte um plano de ação com etapas, responsáveis e riscos:' },
  { label: 'E-mail profissional', prompt: 'Escreva um e-mail profissional e objetivo sobre:' },
  { label: 'Comparar estratégias', prompt: 'Compare as duas estratégias abaixo e recomende:' },
];

const ONBOARDING_KEY = 'ia-onboarding-done';

function AuthDashboard() {
  const { user } = useAuthStore();
  const { conversations, availableModels } = useChatStore();
  const router = useRouter();
  const [promptText, setPromptText] = useState('');

  useEffect(() => {
    if (typeof window !== 'undefined' && !localStorage.getItem(ONBOARDING_KEY)) {
      router.replace('/welcome');
    }
  }, [router]);

  const recentConvs = conversations.slice(0, 4);
  const pinnedConvs = conversations.filter((c) => c.pinned);
  const totalMessages = conversations.reduce((s, c) => s + c.messages.length, 0);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const t = promptText.trim();
    if (!t) return;
    router.push(`/chat?prompt=${encodeURIComponent(t)}`);
  };

  return (
    <div className="min-h-screen bg-[var(--background)]">
      <div className="border-b border-[var(--border)] bg-[var(--surface-1)] px-6 py-5">
        <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
          <p className="text-[var(--text-xs)] text-[var(--muted-foreground)] uppercase tracking-widest font-medium mb-1">
            {new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}
          </p>
          <h1 className="text-[var(--text-2xl)] font-bold">
            Olá, <span className="gradient-text">{user?.fullName?.split(' ')[0] || 'Usuário'}</span> 👋
          </h1>
          <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">
            {conversations.length === 0
              ? 'Comece sua primeira conversa com IA agora.'
              : `Você tem ${conversations.length} conversa${conversations.length !== 1 ? 's' : ''} e ${totalMessages} mensagem${totalMessages !== 1 ? 's' : ''}.`}
          </p>
        </motion.div>
      </div>

      <div className="mx-auto max-w-6xl px-4 md:px-6 py-6 space-y-8">
        <motion.form onSubmit={handleSubmit} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, delay: 0.05 }}
          className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] p-4 shadow-[var(--shadow-md)] focus-within:border-[var(--ring)] focus-within:shadow-[var(--shadow-brand)] transition-all">
          <div className="flex items-end gap-3">
            <textarea value={promptText} onChange={(e) => setPromptText(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSubmit(e as unknown as FormEvent); }}}
              placeholder="O que você quer criar ou analisar hoje?" rows={2}
              className="flex-1 resize-none bg-transparent text-[var(--text-sm)] placeholder:text-[var(--muted-foreground)] focus:outline-none leading-relaxed" />
            <motion.button type="submit" disabled={!promptText.trim()} whileTap={{ scale: 0.94 }}
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-[var(--radius-md)] text-white shadow-[var(--shadow-brand)] hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
              style={{ background: 'var(--brand-gradient)' }}>
              <ArrowUp className="h-4 w-4" />
            </motion.button>
          </div>
          <div className="mt-3 flex flex-wrap gap-1.5">
            {quickActions.map((action) => (
              <button key={action.label} type="button" onClick={() => setPromptText(action.prompt)}
                className="inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] border border-[var(--border)] bg-[var(--surface-2)] px-3 py-1 text-[var(--text-xs)] text-[var(--muted-foreground)] hover:border-[var(--border-strong)] hover:text-[var(--foreground)] transition-colors">
                <WandSparkles className="h-2.5 w-2.5 shrink-0 text-[var(--brand-primary)]" />
                {action.label}
              </button>
            ))}
          </div>
        </motion.form>

        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, delay: 0.1 }}
          className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[
            { label: 'Conversas', value: conversations.length, icon: MessageSquare, color: 'text-[var(--brand-primary)]', bg: 'bg-[var(--brand-primary)]/10' },
            { label: 'Mensagens', value: totalMessages, icon: Activity, color: 'text-[var(--brand-secondary)]', bg: 'bg-[var(--brand-secondary)]/10' },
            { label: 'Fixadas', value: pinnedConvs.length, icon: Star, color: 'text-[var(--warning)]', bg: 'bg-[var(--warning)]/10' },
            { label: 'Modelos', value: availableModels.length || 13, icon: Layers, color: 'text-[var(--success)]', bg: 'bg-[var(--success)]/10' },
          ].map((stat) => (
            <div key={stat.label} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-1)] p-4 flex flex-col gap-3">
              <span className={cn('inline-flex h-8 w-8 items-center justify-center rounded-[var(--radius-md)]', stat.bg)}>
                <stat.icon className={cn('h-4 w-4', stat.color)} />
              </span>
              <div>
                <p className="text-[var(--text-xl)] font-bold tabular-nums">{stat.value}</p>
                <p className="text-[var(--text-xs)] text-[var(--muted-foreground)]">{stat.label}</p>
              </div>
            </div>
          ))}
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, delay: 0.15 }}
          className="grid gap-4 md:grid-cols-3">
          <div className="md:col-span-2 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] p-4">
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-semibold text-[var(--text-base)] flex items-center gap-2">
                <MessageSquare className="h-4 w-4 text-[var(--muted-foreground)]" />
                Conversas recentes
              </h2>
              <Link href="/library" className="flex items-center gap-1 text-[var(--text-xs)] text-[var(--brand-primary)] hover:opacity-80 transition-opacity">
                Ver todas <ChevronRight className="h-3 w-3" />
              </Link>
            </div>
            {recentConvs.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 text-center">
                <MessageSquare className="h-8 w-8 text-[var(--muted-foreground)] mb-2 opacity-40" />
                <p className="text-[var(--text-sm)] text-[var(--muted-foreground)]">Nenhuma conversa ainda.</p>
                <Link href="/chat" className="mt-3 inline-flex items-center gap-1.5 rounded-[var(--radius-md)] bg-[var(--brand-primary)] px-4 py-2 text-[var(--text-xs)] font-medium text-white hover:opacity-90 transition-opacity">
                  <MessageSquare className="h-3.5 w-3.5" /> Iniciar chat
                </Link>
              </div>
            ) : (
              <div className="space-y-0.5">
                {recentConvs.map((conv) => (
                  <Link key={conv.id} href="/chat"
                    className="flex items-center gap-3 rounded-[var(--radius-md)] px-3 py-2.5 hover:bg-[var(--surface-2)] transition-colors group">
                    <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[var(--surface-2)] text-[10px] font-bold text-[var(--muted-foreground)] group-hover:bg-[var(--brand-primary)]/10">
                      {conv.title.slice(0, 2).toUpperCase()}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-[var(--text-sm)] font-medium">{conv.title}</p>
                      <p className="text-[10px] text-[var(--subtle-foreground)]">{conv.model} · {conv.messages.length} msg{conv.messages.length !== 1 ? 's' : ''}</p>
                    </div>
                    <ChevronRight className="h-3.5 w-3.5 text-[var(--muted-foreground)] opacity-0 group-hover:opacity-100 transition-opacity" />
                  </Link>
                ))}
              </div>
            )}
          </div>

          <div className="space-y-2.5">
            {[
              { icon: MessageSquare, label: 'Novo chat', desc: 'IA multi-modelo', href: '/chat', primary: true },
              { icon: BookOpen, label: 'Biblioteca', desc: `${conversations.length} conversas`, href: '/library', primary: false },
              { icon: WandSparkles, label: 'Templates', desc: 'Prompts curados', href: '/prompts', primary: false },
              { icon: BarChart3, label: 'Plano e uso', desc: 'Ver consumo', href: '/billing', primary: false },
            ].map((item) => (
              <Link key={item.label} href={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-[var(--radius-lg)] border border-[var(--border)] px-4 py-3 transition-all hover:shadow-[var(--shadow-md)] hover:-translate-y-0.5 group',
                  item.primary ? 'text-white' : 'bg-[var(--surface-1)] text-[var(--foreground)]'
                )}
                style={item.primary ? { background: 'var(--brand-gradient)' } : undefined}>
                <item.icon className={cn('h-4 w-4 shrink-0', item.primary ? 'text-white' : 'text-[var(--muted-foreground)]')} />
                <div className="min-w-0 flex-1">
                  <p className={cn('text-[var(--text-sm)] font-medium', item.primary ? 'text-white' : '')}>{item.label}</p>
                  <p className={cn('text-[10px]', item.primary ? 'text-white/70' : 'text-[var(--subtle-foreground)]')}>{item.desc}</p>
                </div>
                <ArrowRight className={cn('h-3.5 w-3.5 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity', item.primary ? 'text-white' : 'text-[var(--muted-foreground)]')} />
              </Link>
            ))}
          </div>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3, delay: 0.2 }}>
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold text-[var(--text-base)] flex items-center gap-2">
              <Layers className="h-4 w-4 text-[var(--muted-foreground)]" />
              Modelos disponíveis
            </h2>
            <Link href="/chat" className="flex items-center gap-1 text-[var(--text-xs)] text-[var(--brand-primary)] hover:opacity-80">
              Usar no chat <ChevronRight className="h-3 w-3" />
            </Link>
          </div>
          <div className="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-6 gap-2">
            {[
              { label: 'GPT-4o mini', provider: 'OpenAI', color: '#10a37f' },
              { label: 'Claude Haiku', provider: 'Anthropic', color: '#d4763b' },
              { label: 'Gemini Flash', provider: 'Google', color: '#4285F4' },
              { label: 'DeepSeek', provider: 'DeepSeek', color: '#6366f1' },
              { label: 'Llama 70B', provider: 'Meta', color: '#0667d0' },
              { label: 'Mistral', provider: 'Mistral AI', color: '#fe5b35' },
            ].map((model) => (
              <Link key={model.label} href="/chat"
                className="flex flex-col gap-2 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-1)] p-3 hover:border-[var(--border-strong)] hover:bg-[var(--surface-2)] transition-colors">
                <div className="h-2 w-2 rounded-full" style={{ background: model.color }} />
                <p className="text-[var(--text-xs)] font-semibold leading-tight">{model.label}</p>
                <p className="text-[10px] text-[var(--subtle-foreground)]">{model.provider}</p>
              </Link>
            ))}
          </div>
        </motion.div>
      </div>
    </div>
  );
}

function LandingPage() {
  const router = useRouter();
  const [promptText, setPromptText] = useState('');

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const t = promptText.trim();
    if (!t) return;
    router.push(`/chat?prompt=${encodeURIComponent(t)}`);
  };

  return (
    <main className="min-h-screen bg-[var(--background)] text-[var(--foreground)]">
      <header className="sticky top-0 z-40 backdrop-blur-md bg-[var(--background)]/80 border-b border-[var(--border)]">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between px-5">
          <Link href="/" className="flex items-center gap-2 text-[var(--text-sm)] font-bold">
            <span className="inline-flex h-7 w-7 items-center justify-center rounded-[var(--radius-md)] text-white" style={{ background: 'var(--brand-gradient)' }}>
              <Bot className="h-4 w-4" />
            </span>
            <span className="gradient-text">IA Aggregator</span>
          </Link>
          <nav className="hidden items-center gap-6 text-[var(--text-sm)] font-medium md:flex">
            {[['Biblioteca', '/library'], ['Templates', '/prompts'], ['Plano', '/billing']].map(([label, href]) => (
              <Link key={label} href={href} className="text-[var(--muted-foreground)] hover:text-[var(--foreground)] transition-colors">{label}</Link>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            <Link href="/login" className="px-4 py-2 text-[var(--text-sm)] font-medium text-[var(--foreground)] hover:text-[var(--brand-primary)] transition-colors">
              Entrar
            </Link>
            <Link href="/register" className="rounded-[var(--radius-md)] px-4 py-2 text-[var(--text-sm)] font-medium text-white shadow-[var(--shadow-brand)] hover:opacity-90 transition-opacity"
              style={{ background: 'var(--brand-gradient)' }}>
              Começar grátis
            </Link>
          </div>
        </div>
      </header>

      <section className="mx-auto max-w-6xl px-5 pb-16 pt-20 text-center">
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}>
          <span className="mb-5 inline-flex items-center gap-2 rounded-full border border-[var(--brand-primary)]/30 bg-[var(--brand-primary)]/10 px-4 py-1.5 text-[var(--text-xs)] font-medium text-[var(--brand-primary)]">
            <Sparkles className="h-3.5 w-3.5" />
            13+ modelos IA em um painel — 2026
          </span>
          <h1 className="text-balance text-[clamp(2.25rem,5vw,3.5rem)] font-bold leading-[1.1] tracking-[-0.03em] mt-4">
            Qual tarefa você quer<br />
            <span className="gradient-text">acelerar hoje?</span>
          </h1>
          <p className="mx-auto mt-5 max-w-xl text-[var(--text-base)] text-[var(--muted-foreground)] leading-relaxed">
            Planeje, escreva e execute com GPT-4o, Claude, Gemini, Mistral e mais — tudo em um único workspace inteligente.
          </p>
        </motion.div>

        <motion.form onSubmit={handleSubmit} initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }} className="mx-auto mt-10 max-w-2xl">
          <div className="flex min-h-[64px] items-end gap-2 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] px-4 py-3 shadow-[var(--shadow-lg)] focus-within:border-[var(--ring)] focus-within:shadow-[var(--shadow-brand)] transition-all">
            <textarea value={promptText} onChange={(e) => setPromptText(e.target.value)} rows={1}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSubmit(e as unknown as FormEvent<HTMLFormElement>); }}}
              placeholder="Comece aqui... ou escolha um template abaixo"
              className="flex-1 resize-none bg-transparent text-[var(--text-sm)] placeholder:text-[var(--muted-foreground)] focus:outline-none leading-relaxed"
              style={{ maxHeight: '120px' }} />
            <motion.button type="submit" disabled={!promptText.trim()} whileTap={{ scale: 0.94 }}
              className="mb-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-[var(--radius-md)] text-white shadow-[var(--shadow-brand)] hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition-all"
              style={{ background: 'var(--brand-gradient)' }}>
              <ArrowUp className="h-4 w-4" />
            </motion.button>
          </div>
          <div className="mt-3 flex flex-wrap justify-center gap-2">
            {quickActions.map((action) => (
              <button key={action.label} type="button" onClick={() => setPromptText(action.prompt)}
                className="inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] border border-[var(--border)] bg-[var(--surface-1)] px-3 py-1.5 text-[var(--text-xs)] text-[var(--muted-foreground)] hover:border-[var(--brand-primary)]/50 hover:text-[var(--brand-primary)] transition-colors">
                <WandSparkles className="h-2.5 w-2.5" />{action.label}
              </button>
            ))}
          </div>
        </motion.form>
      </section>

      <section className="border-y border-[var(--border)] bg-[var(--surface-1)]">
        <div className="mx-auto max-w-6xl grid grid-cols-2 divide-x divide-[var(--border)] md:grid-cols-4">
          {statsData.map((stat) => (
            <div key={stat.label} className="flex flex-col items-center gap-1 px-6 py-5 text-center">
              <stat.icon className="h-4 w-4 text-[var(--brand-primary)] mb-1" />
              <p className="text-[var(--text-xl)] font-bold gradient-text">{stat.value}</p>
              <p className="text-[var(--text-xs)] text-[var(--muted-foreground)]">{stat.label}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-5 py-16">
        <div className="mb-10 text-center">
          <h2 className="text-[var(--text-2xl)] font-bold tracking-tight">Tudo que você precisa</h2>
          <p className="mt-2 text-[var(--muted-foreground)] text-[var(--text-sm)]">Ferramentas projetadas para produtividade real</p>
        </div>
        <div className="grid gap-5 md:grid-cols-3">
          {featureCards.map((card, i) => (
            <motion.div key={card.title} initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4, delay: 0.05 * i }}
              className={cn('rounded-[var(--radius-xl)] border border-[var(--border)] bg-gradient-to-br p-6', card.gradient)}>
              <div className="mb-4 inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-lg)] bg-[var(--surface-1)] border border-[var(--border)] shadow-[var(--shadow-sm)]">
                <card.icon className={cn('h-5 w-5', card.iconColor)} />
              </div>
              <h3 className="font-semibold text-[var(--text-base)]">{card.title}</h3>
              <p className="mt-2 text-[var(--text-sm)] text-[var(--muted-foreground)] leading-relaxed">{card.desc}</p>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-5 pb-20">
        <motion.div initial={{ opacity: 0, scale: 0.98 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.4, delay: 0.2 }}
          className="rounded-[var(--radius-2xl)] p-10 text-center text-white" style={{ background: 'var(--brand-gradient)' }}>
          <h2 className="text-[clamp(1.5rem,3vw,2rem)] font-bold">Pronto para acelerar?</h2>
          <p className="mt-2 text-white/80 text-[var(--text-sm)]">Crie sua conta gratuitamente e comece em segundos.</p>
          <div className="mt-6 flex items-center justify-center gap-3 flex-wrap">
            <Link href="/register" className="rounded-[var(--radius-lg)] bg-white px-6 py-3 text-[var(--text-sm)] font-semibold text-[var(--brand-primary)] hover:opacity-90 transition-opacity shadow-lg">
              Criar conta grátis <ArrowRight className="inline h-4 w-4 ml-1" />
            </Link>
            <Link href="/login" className="rounded-[var(--radius-lg)] border border-white/30 px-6 py-3 text-[var(--text-sm)] font-medium text-white hover:bg-white/10 transition-colors">
              Já tenho conta
            </Link>
          </div>
        </motion.div>
      </section>
    </main>
  );
}

export default function HomePage() {
  const { isAuthenticated, isLoading, fetchUser } = useAuthStore();

  useEffect(() => { fetchUser(); }, [fetchUser]);

  if (isLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <div className="flex gap-1"><span className="pulse-dot" /><span className="pulse-dot" /><span className="pulse-dot" /></div>
      </main>
    );
  }

  if (isAuthenticated) return <AuthDashboard />;
  return <LandingPage />;
}
