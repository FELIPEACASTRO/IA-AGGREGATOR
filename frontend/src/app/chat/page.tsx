'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { useRouter, useSearchParams } from 'next/navigation';
import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { cn } from '@/lib/cn';
import { toast } from '@/stores/toast-store';
import { createPerfTimer, trackEvent } from '@/lib/analytics';

function ChatPageContent() {
  const { user, isAuthenticated, isLoading, fetchUser } = useAuthStore();
  const router = useRouter();

  const {
    conversations,
    activeConversationId,
    selectedModel,
    isSending,
    isStreaming,
    availableModels,
    setSelectedModel,
    createConversation,
    setActiveConversation,
    renameConversation,
    toggleConversationPinned,
    clearConversationMessages,
    sendMessage,
    stopGenerating,
    deleteConversation,
  } = useChatStore();

  const [input, setInput] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [copiedMessageId, setCopiedMessageId] = useState<string | null>(null);
  const searchParams = useSearchParams();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const activeConversation = conversations.find((c) => c.id === activeConversationId);
  const filteredConversations = conversations.filter((conversation) =>
    conversation.title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const featuredModels = availableModels.slice(0, 6);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isLoading, isAuthenticated, router]);

  useEffect(() => {
    const prompt = searchParams.get('prompt');
    if (prompt) {
      setInput(prompt);
      inputRef.current?.focus();
    }
  }, [searchParams]);

  useEffect(() => {
    const handleShortcut = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        inputRef.current?.focus();
      }
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'n') {
        event.preventDefault();
        createConversation();
      }
    };

    window.addEventListener('keydown', handleShortcut);
    return () => window.removeEventListener('keydown', handleShortcut);
  }, [createConversation]);

  // Auto-scroll on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeConversation?.messages]);

  // Focus input
  useEffect(() => {
    inputRef.current?.focus();
  }, [activeConversationId]);

  const handleSend = async () => {
    const trimmed = input.trim();
    if (!trimmed || isSending) return;
    const timer = createPerfTimer();
    trackEvent('chat_send_start', {
      model: activeConversation?.model || selectedModel,
      promptLength: trimmed.length,
    });
    setInput('');
    try {
      await sendMessage(trimmed);
      trackEvent('chat_send_success', {
        latencyMs: timer.elapsedMs(),
        model: activeConversation?.model || selectedModel,
      });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'unknown';
      trackEvent('chat_send_error', { latencyMs: timer.elapsedMs(), message });
      throw error;
    }
    inputRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleRetry = async () => {
    if (!activeConversation) return;
    const lastUserMessage = [...activeConversation.messages]
      .reverse()
      .find((message) => message.role === 'user');

    if (!lastUserMessage || isSending) {
      toast.info('Nada para reenviar', 'Envie uma mensagem antes de tentar novamente.');
      return;
    }
    trackEvent('chat_retry');
    await sendMessage(lastUserMessage.content);
    toast.info('Reenviando prompt', 'A última mensagem foi reenviada.');
  };

  const handleCopyMessage = async (id: string, content: string) => {
    try {
      await navigator.clipboard.writeText(content);
      setCopiedMessageId(id);
      setTimeout(() => setCopiedMessageId(null), 1400);
      trackEvent('chat_copy_message');
      toast.success('Copiado', 'Mensagem copiada para a área de transferência.');
    } catch {
      setCopiedMessageId(null);
      toast.error('Falha ao copiar', 'Não foi possível copiar a mensagem.');
    }
  };

  if (isLoading) return <main className="flex min-h-screen items-center justify-center">Carregando...</main>;

  if (!isAuthenticated) return null;

  return (
    <AppShell
      title="Chat IA"
      subtitle={`Olá, ${user?.fullName?.split(' ')[0] || 'usuário'} · ${conversations.length} conversa(s)`}
      headerActions={
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            className="hidden md:inline-flex"
            onClick={handleRetry}
            disabled={!activeConversation || isSending}
          >
            Tentar novamente
          </Button>
          <Button
            variant="ghost"
            className="hidden md:inline-flex"
            onClick={() => {
              if (!activeConversationId) return;
              clearConversationMessages(activeConversationId);
              trackEvent('chat_clear');
              toast.success('Conversa limpa', 'As mensagens da conversa foram removidas.');
            }}
            disabled={!activeConversation || activeConversation.messages.length === 0}
          >
            Limpar
          </Button>
          <Button
            variant="ghost"
            className="hidden md:inline-flex"
            onClick={() => {
              stopGenerating();
              trackEvent('chat_stop_generation');
              toast.info('Geração interrompida');
            }}
            disabled={!isSending}
          >
            Parar
          </Button>
          <select
            value={activeConversation?.model || selectedModel}
            onChange={(e) => setSelectedModel(e.target.value)}
            className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-3 py-2 text-sm"
            aria-label="Selecionar modelo"
          >
            {availableModels.map((model) => (
              <option key={model.id} value={model.id}>
                {model.label}
              </option>
            ))}
          </select>
        </div>
      }
    >
      <div className="flex h-full min-h-0">
        <aside className="hidden w-80 shrink-0 border-r border-[var(--border)] p-4 lg:flex lg:flex-col">
          <Button onClick={() => createConversation()} className="w-full">
            + Nova Conversa
          </Button>
          <Input
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            className="mt-3"
            placeholder="Buscar conversas..."
            aria-label="Buscar conversas"
          />
          <div className="mt-3 min-h-0 flex-1 space-y-1 overflow-y-auto">
            {filteredConversations.length === 0 ? (
              <p className="p-2 text-sm text-[var(--muted-foreground)]">Nenhuma conversa encontrada.</p>
            ) : (
              filteredConversations.map((conversation) => (
                <div
                  key={conversation.id}
                  role="button"
                  tabIndex={0}
                  className={cn(
                    'group w-full rounded-lg border px-3 py-2 text-left transition',
                    activeConversationId === conversation.id
                      ? 'border-[var(--primary)] bg-[var(--secondary)]'
                      : 'border-transparent hover:border-[var(--border)] hover:bg-[var(--secondary)]'
                  )}
                  onClick={() => setActiveConversation(conversation.id)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      setActiveConversation(conversation.id);
                    }
                  }}
                >
                  <div className="flex items-start gap-2">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{conversation.title}</p>
                      <p className="text-xs text-[var(--muted-foreground)]">{conversation.model}</p>
                    </div>
                    <div className="invisible flex items-center gap-2 group-hover:visible">
                      <button
                        type="button"
                        className="cursor-pointer text-xs text-[var(--muted-foreground)]"
                        onClick={(event) => {
                          event.stopPropagation();
                          const title = window.prompt('Renomear conversa', conversation.title);
                          if (title) {
                            renameConversation(conversation.id, title);
                          }
                        }}
                      >
                        Renomear
                      </button>
                      <button
                        type="button"
                        className="cursor-pointer text-xs text-[var(--muted-foreground)]"
                        onClick={(event) => {
                          event.stopPropagation();
                          toggleConversationPinned(conversation.id);
                        }}
                      >
                        {conversation.pinned ? 'Desafixar' : 'Fixar'}
                      </button>
                      <button
                        type="button"
                        className="cursor-pointer text-xs text-[var(--destructive)]"
                        onClick={(event) => {
                          event.stopPropagation();
                          deleteConversation(conversation.id);
                        }}
                      >
                        Excluir
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </aside>

        <section className="flex min-h-0 flex-1 flex-col">
          <div className="flex-1 space-y-4 overflow-y-auto p-4 md:p-6">
            {!activeConversation || activeConversation.messages.length === 0 ? (
              <div className="mx-auto flex h-full w-full max-w-3xl flex-col items-center justify-center text-center">
                <h3 className="text-2xl font-semibold">Como posso ajudar hoje?</h3>
                <p className="mt-2 text-sm text-[var(--muted-foreground)]">
                  Selecione um modelo e envie sua primeira mensagem.
                </p>
                <div className="mt-6 grid w-full gap-2 sm:grid-cols-2 md:grid-cols-3">
                  {featuredModels.map((model) => (
                    <Button
                      key={model.id}
                      variant="secondary"
                      className="h-auto flex-col items-start gap-0 py-3 text-left"
                      onClick={() => {
                        setSelectedModel(model.id);
                        createConversation();
                        inputRef.current?.focus();
                      }}
                    >
                      <span className="text-sm font-medium">{model.label}</span>
                      <span className="text-xs text-[var(--muted-foreground)]">{model.provider}</span>
                    </Button>
                  ))}
                </div>
              </div>
            ) : (
              activeConversation.messages.map((message) => (
                <div
                  key={message.id}
                  className={cn('flex', message.role === 'user' ? 'justify-end' : 'justify-start')}
                >
                  <article
                    className={cn(
                      'max-w-[90%] rounded-2xl px-4 py-3 text-sm whitespace-pre-wrap md:max-w-[75%]',
                      message.role === 'user' && 'bg-[var(--primary)] text-[var(--primary-foreground)]',
                      message.role === 'assistant' && 'bg-[var(--secondary)] text-[var(--foreground)]',
                      message.role === 'error' &&
                        'border border-red-300 bg-red-50 text-[var(--destructive)] dark:border-red-900/60 dark:bg-red-950/20'
                    )}
                  >
                    {message.content}
                    <div className="mt-2 flex items-center justify-between gap-3">
                      <button
                        type="button"
                        className="text-xs text-[var(--muted-foreground)] hover:underline"
                        onClick={() => handleCopyMessage(message.id, message.content)}
                      >
                        {copiedMessageId === message.id ? 'Copiado' : 'Copiar'}
                      </button>
                    </div>
                    {message.role === 'assistant' ? (
                      <footer className="mt-2 flex flex-wrap gap-2 text-xs text-[var(--muted-foreground)]">
                        {message.modelUsed ? (
                          <span className="rounded-full bg-[var(--background)] px-2 py-0.5">
                            {message.modelUsed}
                          </span>
                        ) : null}
                        {message.providerUsed ? (
                          <span className="rounded-full bg-[var(--background)] px-2 py-0.5">
                            {message.providerUsed}
                          </span>
                        ) : null}
                        {message.fallbackUsed ? (
                          <span className="rounded-full bg-yellow-100 px-2 py-0.5 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300">
                            fallback
                          </span>
                        ) : null}
                      </footer>
                    ) : null}
                  </article>
                </div>
              ))
            )}

            {isSending ? (
              <div className="flex justify-start">
                <div className="rounded-2xl bg-[var(--secondary)] px-4 py-3 text-sm text-[var(--muted-foreground)]">
                  {isStreaming ? 'Gerando resposta...' : 'Processando resposta...'}
                </div>
              </div>
            ) : null}
            <div ref={messagesEndRef} />
          </div>

          <div className="border-t border-[var(--border)] p-4">
            <div className="mx-auto flex w-full max-w-4xl items-end gap-3">
              <Textarea
                ref={inputRef}
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={handleKeyDown}
                rows={1}
                placeholder="Digite sua mensagem... (Enter envia, Shift+Enter quebra linha)"
                style={{ maxHeight: '160px' }}
                disabled={isSending}
              />
              <Button
                onClick={handleSend}
                disabled={!input.trim() || isSending}
                className="shrink-0 px-5"
              >
                {isSending ? 'Enviando...' : 'Enviar'}
              </Button>
              <Button
                variant="ghost"
                onClick={() => {
                  stopGenerating();
                  trackEvent('chat_stop_generation');
                  toast.info('Geração interrompida');
                }}
                disabled={!isSending}
                className="shrink-0 md:hidden"
              >
                Parar
              </Button>
            </div>
            <p className="mx-auto mt-2 w-full max-w-4xl text-xs text-[var(--muted-foreground)]">
              Atalhos: Ctrl/Cmd + K (focar input), Ctrl/Cmd + N (nova conversa)
            </p>
          </div>
        </section>
      </div>
    </AppShell>
  );
}

export default function ChatPage() {
  return (
    <Suspense fallback={<main className="flex min-h-screen items-center justify-center">Carregando...</main>}>
      <ChatPageContent />
    </Suspense>
  );
}
