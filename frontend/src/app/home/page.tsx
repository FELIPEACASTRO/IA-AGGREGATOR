'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowRight, Brain, Layers, MessageSquare, TrendingUp } from 'lucide-react';

import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';

const modelStrip = ['GPT', 'Claude', 'Gemini', 'DeepSeek', 'Llama', 'Mistral', 'Perplexity', 'Grok'];

export default function WorkspaceHomePage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuthStore();
  const { conversations, availableModels } = useChatStore();

  const [promptText, setPromptText] = useState('');

  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.replace('/login');
  }, [isAuthenticated, isLoading, router]);

  const recentConversations = conversations.slice(0, 4);
  const totalMessages = conversations.reduce((sum, conversation) => sum + conversation.messages.length, 0);
  const pinnedConversations = conversations.filter((conversation) => conversation.pinned).length;

  const metrics = useMemo(
    () => ([
      { label: 'Conversas', value: conversations.length, icon: MessageSquare },
      { label: 'Mensagens', value: totalMessages, icon: TrendingUp },
      { label: 'Fixadas', value: pinnedConversations, icon: Layers },
      { label: 'Modelos', value: availableModels.length || 13, icon: Brain },
    ]),
    [availableModels.length, conversations.length, pinnedConversations, totalMessages]
  );

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const trimmed = promptText.trim();
    if (!trimmed) return;
    router.push(`/chat?prompt=${encodeURIComponent(trimmed)}`);
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
    <AppShell title="Home" subtitle={`Bem-vindo, ${user?.fullName?.split(' ')[0] || 'time'}.`}>
      <div className="space-y-6 py-6">
        <section className="lume-panel rounded-[var(--radius-2xl)] p-6 md:p-8">
          <div className="grid gap-6 lg:grid-cols-[1.08fr_0.92fr] lg:items-end">
            <div>
              <Badge variant="brand">Workspace home</Badge>
              <h1 className="mt-4 text-[var(--text-4xl)] font-semibold text-[var(--foreground)]">Continue com contexto, ritmo e clareza.</h1>
              <p className="mt-4 max-w-2xl text-[var(--text-base)] leading-8 text-[var(--muted-foreground)]">
                Abra uma conversa recente, execute um template ou dispare um novo prompt com o melhor modelo para a tarefa.
              </p>
            </div>
            <form onSubmit={handleSubmit} className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4 shadow-[var(--shadow-sm)]">
              <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Novo prompt</p>
              <textarea
                value={promptText}
                onChange={(event) => setPromptText(event.target.value)}
                rows={3}
                placeholder="Descreva a tarefa ou cole o contexto aqui"
                className="mt-3 w-full resize-none rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-3 text-[var(--text-sm)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
              />
              <div className="mt-3 flex justify-end">
                <Button variant="brand" size="md" type="submit">Abrir no chat</Button>
              </div>
            </form>
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {metrics.map((metric) => (
            <div key={metric.label} className="lume-panel rounded-[var(--radius-xl)] p-5">
              <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] bg-[rgba(96,115,255,0.12)] text-[var(--brand-primary)]">
                <metric.icon className="h-5 w-5" />
              </span>
              <p className="mt-4 text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">{metric.value}</p>
              <p className="mt-1 text-[0.8rem] text-[var(--muted-foreground)]">{metric.label}</p>
            </div>
          ))}
        </section>

        <section className="grid gap-5 lg:grid-cols-[1.08fr_0.92fr]">
          <div className="lume-panel rounded-[var(--radius-2xl)] p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Conversas recentes</p>
                <h2 className="mt-2 text-[var(--text-2xl)] font-semibold text-[var(--foreground)]">Continue sem perder contexto.</h2>
              </div>
              <Button variant="outline" size="sm" onClick={() => router.push('/library')}>
                Ver biblioteca
              </Button>
            </div>
            <div className="mt-5 space-y-3">
              {recentConversations.length === 0 ? (
                <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-10 text-center">
                  <MessageSquare className="mx-auto h-8 w-8 text-[var(--muted-foreground)]" />
                  <p className="mt-3 text-[var(--text-sm)] text-[var(--muted-foreground)]">Nenhuma conversa por aqui ainda.</p>
                </div>
              ) : (
                recentConversations.map((conversation) => (
                  <button
                    key={conversation.id}
                    type="button"
                    onClick={() => router.push('/chat')}
                    className="flex w-full items-center gap-4 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-4 text-left hover:border-[var(--border-strong)]"
                  >
                    <span className="inline-flex h-11 w-11 items-center justify-center rounded-full bg-[rgba(96,115,255,0.12)] text-[0.82rem] font-semibold text-[var(--brand-primary)]">
                      {conversation.title.slice(0, 2).toUpperCase()}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-[var(--text-sm)] font-semibold text-[var(--foreground)]">{conversation.title}</span>
                      <span className="block truncate text-[0.74rem] text-[var(--muted-foreground)]">{conversation.model} · {conversation.messages.length} mensagens</span>
                    </span>
                    <ArrowRight className="h-4 w-4 text-[var(--subtle-foreground)]" />
                  </button>
                ))
              )}
            </div>
          </div>

          <div className="space-y-5">
            <div className="lume-panel rounded-[var(--radius-2xl)] p-6">
              <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Atalhos</p>
              <div className="mt-4 space-y-3">
                {[
                  { label: 'Abrir chat', href: '/chat' },
                  { label: 'Ver templates', href: '/prompts' },
                  { label: 'Plano e consumo', href: '/billing' },
                  { label: 'Configuracoes', href: '/settings' },
                ].map((item) => (
                  <Link key={item.href} href={item.href} className="flex items-center justify-between rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-3 text-[var(--text-sm)] font-medium text-[var(--foreground)] hover:border-[var(--border-strong)]">
                    {item.label}
                    <ArrowRight className="h-4 w-4 text-[var(--subtle-foreground)]" />
                  </Link>
                ))}
              </div>
            </div>
            <div className="lume-panel rounded-[var(--radius-2xl)] p-6">
              <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Modelos ativos</p>
              <div className="mt-4 flex flex-wrap gap-2">
                {(availableModels.slice(0, 8).length > 0 ? availableModels.slice(0, 8) : modelStrip).map((model) => (
                  <span key={typeof model === 'string' ? model : model.id} className="rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-2 text-[0.76rem] font-medium text-[var(--foreground)]">
                    {typeof model === 'string' ? model : model.label}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </section>
      </div>
    </AppShell>
  );
}

