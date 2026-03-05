'use client';

import { Suspense, useEffect, useMemo, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import { motion, AnimatePresence } from 'framer-motion';
import { DndContext, DragEndEvent, closestCenter, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import { SortableContext, useSortable, rectSortingStrategy, arrayMove } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore, ChatMessage } from '@/stores/chat-store';
import { useRouter, useSearchParams } from 'next/navigation';
import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Dropdown, DropdownOption } from '@/components/ui/dropdown';
import { ProgressBar } from '@/components/ui/progress-bar';
import { cn } from '@/lib/cn';
import { toast } from '@/stores/toast-store';
import { createPerfTimer, trackEvent } from '@/lib/analytics';
import {
  ArrowUp, Plus, WandSparkles, Copy, Check, RefreshCw,
  ThumbsUp, ThumbsDown, LayoutGrid, MessageSquare,
  Pin, Trash2, Pencil, MoreHorizontal,
  Zap, Brain, Scale, GripVertical,
} from 'lucide-react';

type Tier = 'fast' | 'balanced' | 'powerful';

const MODEL_META: Record<string, { tier: Tier; color: string }> = {
  'gpt-4o-mini':             { tier: 'fast',      color: '#10a37f' },
  'gpt-4.1-mini':            { tier: 'fast',      color: '#10a37f' },
  'claude-3-5-haiku':        { tier: 'fast',      color: '#d4763b' },
  'gemini-1.5-flash':        { tier: 'fast',      color: '#4285F4' },
  'deepseek-chat':           { tier: 'balanced',  color: '#6366f1' },
  'deepseek-reasoner':       { tier: 'powerful',  color: '#6366f1' },
  'llama-3.1-8b-instant':    { tier: 'fast',      color: '#0667d0' },
  'llama-3.1-70b-versatile': { tier: 'balanced',  color: '#0667d0' },
  'mistral-small-latest':    { tier: 'fast',      color: '#fe5b35' },
  'mistral-large-latest':    { tier: 'balanced',  color: '#fe5b35' },
  'command-r':               { tier: 'balanced',  color: '#39d353' },
  'command-r-plus':          { tier: 'powerful',  color: '#39d353' },
  'sonar':                   { tier: 'powerful',  color: '#20b2aa' },
};

const TIER_CONFIG: Record<Tier, { icon: React.ElementType; label: string; color: string }> = {
  fast:     { icon: Zap,   label: 'Rapido',      color: 'text-[var(--success)]'       },
  balanced: { icon: Scale, label: 'Equilibrado',  color: 'text-[var(--warning)]'       },
  powerful: { icon: Brain, label: 'Poderoso',     color: 'text-[var(--brand-primary)]' },
};

const quickActions = [
  'Resuma os pontos principais em 5 bullets executivos.',
  'Monte um plano de acao com etapas, responsaveis e riscos.',
  'Reescreva em tom profissional e objetivo.',
  'Compare duas alternativas e recomende com justificativa.',
];

function StreamingIndicator({ model }: { model?: string }) {
  const color = model ? MODEL_META[model]?.color : undefined;
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
      className="flex items-center gap-3 rounded-2xl bg-[var(--surface-1)] border border-[var(--border)] px-4 py-3 max-w-fit"
    >
      <div className="flex items-center gap-1 h-4">
        <span className="pulse-dot" style={color ? { background: color } : undefined} />
        <span className="pulse-dot" style={color ? { background: color } : undefined} />
        <span className="pulse-dot" style={color ? { background: color } : undefined} />
      </div>
      <span className="text-[var(--text-xs)] text-[var(--muted-foreground)]">Gerando resposta</span>
    </motion.div>
  );
}

function MessageContent({ content, role }: { content: string; role: string }) {
  if (role !== 'assistant') {
    return <p className="text-[var(--text-sm)] whitespace-pre-wrap leading-relaxed">{content}</p>;
  }
  return (
    <div className="prose-chat">
      <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>
        {content}
      </ReactMarkdown>
    </div>
  );
}

function MessageActions({
  message, isCopied, onCopy, onRegenerate, onFeedback, feedback,
}: {
  message: ChatMessage;
  isCopied: boolean;
  onCopy: () => void;
  onRegenerate?: () => void;
  onFeedback?: (type: 'up' | 'down') => void;
  feedback?: 'up' | 'down' | null;
}) {
  return (
    <div className="mt-2 flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-[var(--dur-fast)]">
      <button onClick={onCopy} title="Copiar mensagem"
        className="inline-flex items-center gap-1.5 rounded-[var(--radius-sm)] px-2 py-1 text-[var(--text-xs)] text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)] transition-colors">
        {isCopied ? <Check className="h-3 w-3 text-[var(--success)]" /> : <Copy className="h-3 w-3" />}
        {isCopied ? 'Copiado!' : 'Copiar'}
      </button>
      {message.role === 'assistant' && onRegenerate && (
        <button onClick={onRegenerate} title="Regenerar"
          className="inline-flex items-center gap-1.5 rounded-[var(--radius-sm)] px-2 py-1 text-[var(--text-xs)] text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)] transition-colors">
          <RefreshCw className="h-3 w-3" />Regenerar
        </button>
      )}
      {message.role === 'assistant' && onFeedback && (
        <>
          <button onClick={() => onFeedback('up')} title="Util"
            className={cn('rounded-[var(--radius-sm)] p-1.5 transition-colors',
              feedback === 'up' ? 'text-[var(--success)] bg-[var(--surface-2)]'
                : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--success)]')}>
            <ThumbsUp className="h-3 w-3" />
          </button>
          <button onClick={() => onFeedback('down')} title="Nao util"
            className={cn('rounded-[var(--radius-sm)] p-1.5 transition-colors',
              feedback === 'down' ? 'text-[var(--destructive)] bg-[var(--surface-2)]'
                : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--destructive)]')}>
            <ThumbsDown className="h-3 w-3" />
          </button>
        </>
      )}
    </div>
  );
}

function CanvasCard({ message, isCopied, onCopy }: { message: ChatMessage; isCopied: boolean; onCopy: () => void }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: message.id });
  return (
    <div ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition, opacity: isDragging ? 0.5 : 1 }}
      className={cn('relative rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-1)] p-4 shadow-[var(--shadow-md)] transition-shadow hover:shadow-[var(--shadow-lg)]',
        message.role === 'user' && 'border-[var(--brand-primary)]/30',
        isDragging && 'shadow-[var(--shadow-xl)] z-10')}>
      <div className="flex items-center gap-2 mb-3">
        <div {...attributes} {...listeners} className="cursor-grab active:cursor-grabbing text-[var(--muted-foreground)] touch-none">
          <GripVertical className="h-4 w-4" />
        </div>
        <span className={cn('inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-bold',
          message.role === 'user' ? 'bg-[var(--brand-primary)] text-white' : 'bg-[var(--surface-2)] text-[var(--muted-foreground)]')}>
          {message.role === 'user' ? 'U' : 'IA'}
        </span>
        <span className="text-[10px] text-[var(--subtle-foreground)]">
          {new Date(message.timestamp).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
        </span>
      </div>
      <div className="pl-6 text-[var(--text-sm)] line-clamp-6 leading-relaxed">
        <MessageContent content={message.content} role={message.role} />
      </div>
      <button onClick={onCopy}
        className="absolute top-2 right-2 rounded-md p-1.5 text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] transition-colors">
        {isCopied ? <Check className="h-3 w-3 text-[var(--success)]" /> : <Copy className="h-3 w-3" />}
      </button>
    </div>
  );
}

function ContextIndicator({ messages }: { messages: Array<{ content: string }> }) {
  const MAX_TOKENS = 8000;
  const used = messages.reduce((s, m) => s + Math.round(m.content.length / 4), 0);
  const pct = Math.min(100, Math.round((used / MAX_TOKENS) * 100));
  return (
    <div className="flex items-center gap-2 min-w-[120px]" title={`Contexto: ~${used} / ${MAX_TOKENS} tokens (${pct}%)`}>
      <span className={cn('text-[10px] tabular-nums shrink-0',
        pct >= 95 ? 'text-[var(--destructive)]' : pct >= 80 ? 'text-[var(--warning)]' : 'text-[var(--muted-foreground)]')}>
        {pct}%
      </span>
      <ProgressBar value={pct} size="sm" className="flex-1" />
    </div>
  );
}

function ConversationItem({ conversation, isActive, onSelect, onRename, onPin, onDelete }: {
  conversation: { id: string; title: string; model: string; pinned: boolean };
  isActive: boolean; onSelect: () => void; onRename: () => void; onPin: () => void; onDelete: () => void;
}) {
  const [menuOpen, setMenuOpen] = useState(false);
  return (
    <div role="button" tabIndex={0} onClick={onSelect}
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onSelect(); } }}
      className={cn('group relative flex w-full cursor-pointer items-start gap-2 rounded-[var(--radius-md)] px-3 py-2 text-left transition-all select-none',
        isActive ? 'bg-[var(--surface-3)] translate-x-0.5' : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)]')}>
      <div className="min-w-0 flex-1 overflow-hidden">
        <p className={cn('truncate text-[var(--text-sm)] font-medium leading-snug',
          isActive ? 'text-[var(--brand-primary)]' : 'text-[var(--foreground)]')}>
          {conversation.title}
        </p>
        <div className="mt-0.5 flex items-center gap-1.5">
          {conversation.pinned && <Pin className="h-2.5 w-2.5 shrink-0 text-[var(--brand-primary)]" />}
          <p className="truncate text-[10px] text-[var(--subtle-foreground)]">{conversation.model}</p>
        </div>
      </div>
      <div className="relative shrink-0">
        <button onClick={(e) => { e.stopPropagation(); setMenuOpen((v) => !v); }}
          className={cn('rounded-[var(--radius-sm)] p-1 text-[var(--muted-foreground)] transition-colors opacity-0 group-hover:opacity-100',
            isActive && 'opacity-100', 'hover:bg-[var(--surface-2)] hover:text-[var(--foreground)]')}
          aria-label="Opcoes">
          <MoreHorizontal className="h-3.5 w-3.5" />
        </button>
        <AnimatePresence>
          {menuOpen && (
            <>
              <div className="fixed inset-0 z-40" onClick={(e) => { e.stopPropagation(); setMenuOpen(false); }} />
              <motion.div initial={{ opacity: 0, scale: 0.95, y: -4 }} animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: -4 }} transition={{ duration: 0.12 }}
                className="glass absolute right-0 top-8 z-50 min-w-[140px] rounded-[var(--radius-md)] shadow-[var(--shadow-lg)] overflow-hidden">
                <button onClick={(e) => { e.stopPropagation(); setMenuOpen(false); onRename(); }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-[var(--text-xs)] text-[var(--foreground)] hover:bg-[var(--surface-2)] transition-colors">
                  <Pencil className="h-3 w-3" /> Renomear
                </button>
                <button onClick={(e) => { e.stopPropagation(); setMenuOpen(false); onPin(); }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-[var(--text-xs)] text-[var(--foreground)] hover:bg-[var(--surface-2)] transition-colors">
                  <Pin className="h-3 w-3" /> {conversation.pinned ? 'Desafixar' : 'Fixar'}
                </button>
                <div className="my-0.5 h-px bg-[var(--border)]" />
                <button onClick={(e) => { e.stopPropagation(); setMenuOpen(false); onDelete(); }}
                  className="flex w-full items-center gap-2 px-3 py-2 text-[var(--text-xs)] text-[var(--destructive)] hover:bg-[var(--surface-2)] transition-colors">
                  <Trash2 className="h-3 w-3" /> Excluir
                </button>
              </motion.div>
            </>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}

function ChatPageContent() {
  const { user, isAuthenticated, isLoading, fetchUser } = useAuthStore();
  const router = useRouter();
  const searchParams = useSearchParams();

  const {
    conversations, activeConversationId, selectedModel,
    isSending, isStreaming, availableModels,
    setSelectedModel, createConversation, setActiveConversation,
    renameConversation, toggleConversationPinned, clearConversationMessages,
    sendMessage, stopGenerating, deleteConversation,
  } = useChatStore();

  const [input, setInput] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [feedbacks, setFeedbacks] = useState<Record<string, 'up' | 'down'>>({});
  const [canvasMode, setCanvasMode] = useState(false);
  const [canvasOrder, setCanvasOrder] = useState<string[]>([]);
  const [recentPrompts, setRecentPrompts] = useState<string[]>([]);
  const [promptHistoryIdx, setPromptHistoryIdx] = useState(-1);
  const [draftBeforeHistory, setDraftBeforeHistory] = useState('');

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const activeConversation = useMemo(
    () => conversations.find((c) => c.id === activeConversationId),
    [conversations, activeConversationId],
  );
  const currentModel = activeConversation?.model ?? selectedModel;

  const filteredConversations = useMemo(
    () => conversations.filter((c) => c.title.toLowerCase().includes(searchTerm.toLowerCase())),
    [conversations, searchTerm],
  );

  const featuredModels = availableModels.slice(0, 6);

  const modelOptions: DropdownOption[] = useMemo(
    () => availableModels.map((m) => {
      const meta = MODEL_META[m.id];
      const tier = meta?.tier ?? 'balanced';
      const TierIcon = TIER_CONFIG[tier].icon;
      return {
        value: m.id, label: m.label, description: m.provider,
        badge: (
          <span className={cn('inline-flex items-center gap-1 text-[10px]', TIER_CONFIG[tier].color)}>
            <TierIcon className="h-2.5 w-2.5" />{TIER_CONFIG[tier].label}
          </span>
        ),
      };
    }),
    [availableModels],
  );

  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 8 } }));

  useEffect(() => { fetchUser(); }, [fetchUser]);
  useEffect(() => {
    if (!isLoading && !isAuthenticated) router.push('/login');
  }, [isLoading, isAuthenticated, router]);
  useEffect(() => {
    const prompt = searchParams.get('prompt');
    if (prompt) { setInput(prompt); inputRef.current?.focus(); }
  }, [searchParams]);
  useEffect(() => {
    if (activeConversation) setCanvasOrder(activeConversation.messages.map((m) => m.id));
  }, [activeConversation?.id, activeConversation?.messages.length]);
  useEffect(() => {
    if (!canvasMode) messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeConversation?.messages, canvasMode]);
  useEffect(() => {
    if (activeConversationId) inputRef.current?.focus();
  }, [activeConversationId]);
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        createConversation();
        setCanvasMode(false);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [createConversation]);

  const handleSend = async () => {
    const trimmed = input.trim();
    if (!trimmed || isSending) return;
    const timer = createPerfTimer();
    trackEvent('chat_send_start', { model: currentModel, promptLength: trimmed.length });
    setRecentPrompts((prev) => [trimmed, ...prev.filter((p) => p !== trimmed)].slice(0, 20));
    setPromptHistoryIdx(-1);
    setInput('');
    try {
      await sendMessage(trimmed);
      trackEvent('chat_send_success', { latencyMs: timer.elapsedMs(), model: currentModel });
    } catch (err: unknown) {
      trackEvent('chat_send_error', { latencyMs: timer.elapsedMs(), message: err instanceof Error ? err.message : 'unknown' });
    }
    inputRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(); return; }
    if (e.key === 'ArrowUp' && input === '' && recentPrompts.length > 0) {
      e.preventDefault();
      const idx = Math.min(promptHistoryIdx + 1, recentPrompts.length - 1);
      if (promptHistoryIdx === -1) setDraftBeforeHistory(input);
      setPromptHistoryIdx(idx);
      setInput(recentPrompts[idx]);
    }
    if (e.key === 'ArrowDown' && promptHistoryIdx > -1) {
      e.preventDefault();
      const idx = promptHistoryIdx - 1;
      setPromptHistoryIdx(idx);
      setInput(idx === -1 ? draftBeforeHistory : recentPrompts[idx]);
    }
  };

  const handleRetry = async () => {
    if (!activeConversation || isSending) return;
    const last = [...activeConversation.messages].reverse().find((m) => m.role === 'user');
    if (!last) { toast.info('Nada para reenviar', 'Envie uma mensagem primeiro.'); return; }
    trackEvent('chat_retry');
    await sendMessage(last.content);
    toast.info('Reenviando prompt');
  };

  const handleCopy = async (id: string, content: string) => {
    try {
      await navigator.clipboard.writeText(content);
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 1600);
      trackEvent('chat_copy_message');
      toast.success('Copiado');
    } catch {
      toast.error('Falha ao copiar', 'Permissao negada pelo navegador.');
    }
  };

  const handleFeedback = (msgId: string, type: 'up' | 'down') => {
    setFeedbacks((prev) => ({ ...prev, [msgId]: type }));
    trackEvent('chat_feedback', { type });
    toast.success(type === 'up' ? 'Obrigado pelo feedback!' : 'Feedback registrado');
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      setCanvasOrder((items) => {
        const from = items.indexOf(String(active.id));
        const to = items.indexOf(String(over.id));
        return arrayMove(items, from, to);
      });
    }
  };

  if (isLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <div className="flex gap-1"><span className="pulse-dot" /><span className="pulse-dot" /><span className="pulse-dot" /></div>
      </main>
    );
  }
  if (!isAuthenticated) return null;

  const canvasMessages = canvasMode && activeConversation
    ? canvasOrder.map((id) => activeConversation.messages.find((m) => m.id === id)).filter((m): m is ChatMessage => !!m)
    : activeConversation?.messages ?? [];

  return (
    <AppShell
      title="Chat"
      subtitle={`${user?.fullName?.split(' ')[0] || 'Usuario'} · ${conversations.length} conversa${conversations.length !== 1 ? 's' : ''}`}
      noPadding
      headerActions={
        <div className="flex items-center gap-2 flex-wrap">
          {activeConversation && activeConversation.messages.length > 0 && (
            <ContextIndicator messages={activeConversation.messages} />
          )}
          <div className="flex rounded-[var(--radius-md)] border border-[var(--border)] overflow-hidden">
            <button onClick={() => setCanvasMode(false)}
              className={cn('flex items-center gap-1.5 px-2.5 py-1.5 text-[var(--text-xs)] transition-colors',
                !canvasMode ? 'bg-[var(--brand-primary)] text-white' : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-2)]')}>
              <MessageSquare className="h-3.5 w-3.5" />Chat
            </button>
            <button onClick={() => setCanvasMode(true)}
              className={cn('flex items-center gap-1.5 px-2.5 py-1.5 text-[var(--text-xs)] transition-colors',
                canvasMode ? 'bg-[var(--brand-primary)] text-white' : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-2)]')}>
              <LayoutGrid className="h-3.5 w-3.5" />Canvas
            </button>
          </div>
          <Button variant="ghost" size="sm" onClick={handleRetry} disabled={!activeConversation || isSending} className="hidden sm:inline-flex">
            Tentar novamente
          </Button>
          <Button variant="ghost" size="sm"
            onClick={() => { if (!activeConversationId) return; clearConversationMessages(activeConversationId); trackEvent('chat_clear'); toast.success('Conversa limpa'); }}
            disabled={!activeConversation || activeConversation.messages.length === 0}
            className="hidden sm:inline-flex">
            Limpar
          </Button>
          {isSending && (
            <Button variant="ghost" size="sm" onClick={() => { stopGenerating(); trackEvent('chat_stop_generation'); }}>Parar</Button>
          )}
          <Dropdown options={modelOptions} value={currentModel} onChange={(val) => { setSelectedModel(val); trackEvent('chat_change_model', { model: val }); }} triggerClassName="h-8 min-w-[140px] text-xs" />
        </div>
      }
    >
      <div className="flex" style={{ height: 'calc(100vh - 128px)' }}>
        <aside className="hidden w-72 shrink-0 border-r border-[var(--border)] flex-col overflow-hidden lg:flex bg-[var(--surface-1)]">
          <div className="p-3 border-b border-[var(--border)]">
            <button onClick={() => { createConversation(); setCanvasMode(false); }}
              className="flex w-full items-center justify-center gap-2 rounded-[var(--radius-md)] bg-[var(--brand-primary)] px-3 py-2 text-[var(--text-sm)] font-medium text-white hover:opacity-90 active:scale-[0.98] transition-all">
              <Plus className="h-4 w-4" />Nova Conversa
            </button>
          </div>
          <div className="px-3 py-2 border-b border-[var(--border)]">
            <div className="relative">
              <input value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} placeholder="Buscar conversas..."
                className="w-full rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-3 py-1.5 pl-8 text-[var(--text-sm)] placeholder:text-[var(--muted-foreground)] focus:outline-none focus:ring-1 focus:ring-[var(--ring)]" />
              <Pin className="pointer-events-none absolute left-2.5 top-2 h-3.5 w-3.5 text-[var(--muted-foreground)]" />
            </div>
          </div>
          <div className="flex-1 overflow-y-auto p-2 space-y-0.5">
            {filteredConversations.length === 0 ? (
              <p className="px-3 py-6 text-center text-[var(--text-xs)] text-[var(--muted-foreground)]">Nenhuma conversa encontrada.</p>
            ) : (
              filteredConversations.map((conv) => (
                <ConversationItem key={conv.id} conversation={conv} isActive={conv.id === activeConversationId}
                  onSelect={() => { setActiveConversation(conv.id); setCanvasMode(false); }}
                  onRename={() => { const t = window.prompt('Renomear:', conv.title); if (t?.trim()) renameConversation(conv.id, t.trim()); }}
                  onPin={() => toggleConversationPinned(conv.id)}
                  onDelete={() => { if (window.confirm('Excluir conversa?')) { deleteConversation(conv.id); toast.success('Conversa excluida'); } }}
                />
              ))
            )}
          </div>
        </aside>

        <section className="flex min-h-0 flex-1 flex-col overflow-hidden">
          <div className="flex-1 overflow-y-auto">
            {canvasMode && activeConversation && activeConversation.messages.length > 0 ? (
              <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                <SortableContext items={canvasOrder} strategy={rectSortingStrategy}>
                  <div className="canvas-viewport min-h-full p-6">
                    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                      {canvasMessages.map((msg) => (
                        <CanvasCard key={msg.id} message={msg} isCopied={copiedId === msg.id} onCopy={() => handleCopy(msg.id, msg.content)} />
                      ))}
                    </div>
                  </div>
                </SortableContext>
              </DndContext>
            ) : !activeConversation || activeConversation.messages.length === 0 ? (
              <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
                className="flex h-full flex-col items-center justify-center p-6 text-center">
                <h2 className="text-[var(--text-2xl)] font-semibold gradient-text">Como posso ajudar hoje?</h2>
                <p className="mt-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">Selecione um modelo abaixo ou envie sua mensagem.</p>
                <div className="mt-6 grid w-full max-w-2xl gap-2 sm:grid-cols-2 md:grid-cols-3">
                  {featuredModels.map((model) => {
                    const meta = MODEL_META[model.id];
                    const tier = meta?.tier ?? 'balanced';
                    const TierIcon = TIER_CONFIG[tier].icon;
                    return (
                      <motion.button key={model.id} whileHover={{ y: -2 }} whileTap={{ scale: 0.97 }}
                        onClick={() => { setSelectedModel(model.id); createConversation(); setTimeout(() => inputRef.current?.focus(), 50); }}
                        className="flex flex-col items-start gap-1 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-1)] px-4 py-3 text-left transition-colors hover:border-[var(--border-strong)] hover:bg-[var(--surface-2)]">
                        <span className="text-[var(--text-sm)] font-semibold">{model.label}</span>
                        <span className="text-[var(--text-xs)] text-[var(--muted-foreground)]">{model.provider}</span>
                        <span className={cn('mt-1 inline-flex items-center gap-1 text-[10px]', TIER_CONFIG[tier].color)}>
                          <TierIcon className="h-2.5 w-2.5" />{TIER_CONFIG[tier].label}
                        </span>
                      </motion.button>
                    );
                  })}
                </div>
              </motion.div>
            ) : (
              <div className="space-y-4 p-4 md:p-6 max-w-4xl mx-auto w-full">
                <AnimatePresence initial={false}>
                  {activeConversation.messages.map((msg) => (
                    <motion.div key={msg.id} initial={{ opacity: 0, y: 12, scale: 0.98 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }} transition={{ duration: 0.2, ease: [0.4, 0, 0.2, 1] }}
                      className={cn('flex group', msg.role === 'user' ? 'justify-end' : 'justify-start')}>
                      <article className={cn('max-w-[88%] rounded-2xl px-4 py-3 md:max-w-[72%]',
                        msg.role === 'user' && 'rounded-br-md bg-[var(--brand-primary)] text-white',
                        msg.role === 'assistant' && 'rounded-bl-md bg-[var(--surface-1)] border border-[var(--border)] text-[var(--foreground)]',
                        msg.role === 'error' && 'border border-[var(--destructive)]/30 bg-[var(--destructive)]/5 text-[var(--destructive)]')}>
                        <MessageContent content={msg.content} role={msg.role} />
                        {msg.role === 'assistant' && (msg.modelUsed || msg.providerUsed || msg.fallbackUsed) && (
                          <div className="mt-2 flex flex-wrap gap-1.5">
                            {msg.modelUsed && <span className="inline-flex items-center rounded-full border border-[var(--border)] bg-[var(--surface-2)] px-2 py-0.5 text-[10px] text-[var(--muted-foreground)]">{msg.modelUsed}</span>}
                            {msg.providerUsed && <span className="inline-flex items-center rounded-full border border-[var(--border)] bg-[var(--surface-2)] px-2 py-0.5 text-[10px] text-[var(--muted-foreground)]">{msg.providerUsed}</span>}
                            {msg.fallbackUsed && <span className="inline-flex items-center rounded-full bg-[var(--warning)]/15 px-2 py-0.5 text-[10px] text-[var(--warning)]">fallback</span>}
                          </div>
                        )}
                        <MessageActions message={msg} isCopied={copiedId === msg.id}
                          onCopy={() => handleCopy(msg.id, msg.content)}
                          onRegenerate={msg.role === 'assistant' ? handleRetry : undefined}
                          onFeedback={(type) => handleFeedback(msg.id, type)}
                          feedback={feedbacks[msg.id]} />
                      </article>
                    </motion.div>
                  ))}
                </AnimatePresence>
                <AnimatePresence>
                  {isSending && (
                    <div className="flex justify-start">
                      <StreamingIndicator model={currentModel} />
                    </div>
                  )}
                </AnimatePresence>
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          <div className="border-t border-[var(--border)] bg-[var(--background)] p-3 md:p-4">
            <div className="mx-auto w-full max-w-3xl">
              <div className="flex min-h-[56px] items-end gap-2 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] px-3 py-2.5 shadow-[var(--shadow-md)] transition-all focus-within:border-[var(--ring)] focus-within:shadow-[var(--shadow-brand)]">
                <button type="button" aria-label="Anexar arquivo"
                  className="mb-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-[var(--radius-md)] text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)] transition-colors">
                  <Plus className="h-4 w-4" />
                </button>
                <Textarea ref={inputRef} value={input} onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown} rows={1} placeholder="Envie uma mensagem..."
                  style={{ maxHeight: '160px' }} disabled={isSending}
                  className="flex-1 border-0 bg-transparent p-0 text-[var(--text-sm)] focus:ring-0 resize-none min-h-8" />
                <motion.button type="button" onClick={handleSend} disabled={!input.trim() || isSending}
                  whileTap={{ scale: 0.94 }} aria-label="Enviar mensagem"
                  className="mb-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-[var(--radius-md)] bg-[var(--brand-primary)] text-white shadow-[var(--shadow-brand)] transition-all hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none">
                  <ArrowUp className="h-4 w-4" />
                </motion.button>
              </div>
              <div className="mt-2.5 flex flex-wrap gap-1.5">
                {quickActions.map((action) => (
                  <button key={action} type="button" onClick={() => { setInput(action); inputRef.current?.focus(); }}
                    className="inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] border border-[var(--border)] bg-[var(--surface-1)] px-3 py-1.5 text-[var(--text-xs)] text-[var(--muted-foreground)] transition-colors hover:border-[var(--border-strong)] hover:text-[var(--foreground)]">
                    <WandSparkles className="h-3 w-3 shrink-0 text-[var(--brand-primary)]" />
                    <span className="truncate max-w-[200px]">{action}</span>
                  </button>
                ))}
              </div>
              <p className="mt-1.5 text-center text-[10px] text-[var(--subtle-foreground)]">
                Seta cima/baixo = historico · Enter envia · Shift+Enter = nova linha · Ctrl+N = nova conversa
              </p>
            </div>
          </div>
        </section>
      </div>
    </AppShell>
  );
}

export default function ChatPage() {
  return (
    <Suspense fallback={
      <main className="flex min-h-screen items-center justify-center">
        <div className="flex gap-1"><span className="pulse-dot" /><span className="pulse-dot" /><span className="pulse-dot" /></div>
      </main>
    }>
      <ChatPageContent />
    </Suspense>
  );
}
