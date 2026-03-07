'use client';

import { useMemo, useState } from 'react';
import { useChatStore } from '@/stores/chat-store';
import { useRouter } from 'next/navigation';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';
import { cn } from '@/lib/cn';
import { AppLayout } from '@/components/app/app-layout';
import {
  BookOpen,
  ChevronRight,
  Clock3,
  LayoutGrid,
  List,
  MessageSquare,
  Pin,
  Search,
  Trash2,
} from 'lucide-react';

type SortKey = 'recent' | 'name' | 'size';
type ViewMode = 'grid' | 'list';

export default function LibraryPage() {
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState<SortKey>('recent');
  const [view, setView] = useState<ViewMode>('list');
  const [filterPinned, setFilterPinned] = useState(false);
  const router = useRouter();
  const { conversations, setActiveConversation, toggleConversationPinned, deleteConversation } = useChatStore();

  const pinnedCount = useMemo(() => conversations.filter((c) => c.pinned).length, [conversations]);

  const filtered = useMemo(() => {
    let list = conversations.filter((c) =>
      c.title.toLowerCase().includes(query.toLowerCase()),
    );
    if (filterPinned) list = list.filter((c) => c.pinned);
    if (sort === 'name') list = [...list].sort((a, b) => a.title.localeCompare(b.title));
    else if (sort === 'size') list = [...list].sort((a, b) => b.messages.length - a.messages.length);
    else list = [...list].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
    return list;
  }, [conversations, filterPinned, query, sort]);

  const openConversation = (id: string) => {
    setActiveConversation(id);
    trackEvent('library_open_conversation', { conversationId: id });
    router.push('/chat');
  };

  const removeConversation = (id: string) => {
    if (!window.confirm('Excluir esta conversa?')) return;
    deleteConversation(id);
    toast.success('Conversa excluida');
  };

  return (
    <AppLayout>
      <div className="mx-auto max-w-4xl px-6 py-8 space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-[24px] font-semibold text-[var(--foreground)]">Biblioteca</h1>
            <p className="mt-1 text-[14px] text-[var(--muted-foreground)]">
              {conversations.length} conversa{conversations.length !== 1 ? 's' : ''} salvas
            </p>
          </div>
          <div className="flex items-center gap-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-[var(--muted-foreground)]" />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Buscar..."
                className="h-9 w-48 rounded-[var(--radius-md)] border border-[var(--input-border)] bg-[var(--input-bg)] pl-9 pr-3 text-[13px] text-[var(--foreground)] outline-none focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--ring)]"
              />
            </div>
            <div className="flex rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)]">
              <button
                onClick={() => setView('list')}
                className={cn(
                  'inline-flex h-9 items-center gap-1.5 px-3 text-[12px] font-medium transition-colors rounded-l-[var(--radius-md)]',
                  view === 'list' ? 'bg-[var(--surface-hover)] text-[var(--foreground)]' : 'text-[var(--muted-foreground)]',
                )}
              >
                <List className="h-3.5 w-3.5" />
              </button>
              <button
                onClick={() => setView('grid')}
                className={cn(
                  'inline-flex h-9 items-center gap-1.5 px-3 text-[12px] font-medium transition-colors rounded-r-[var(--radius-md)]',
                  view === 'grid' ? 'bg-[var(--surface-hover)] text-[var(--foreground)]' : 'text-[var(--muted-foreground)]',
                )}
              >
                <LayoutGrid className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setFilterPinned((v) => !v)}
            className={cn(
              'inline-flex items-center gap-1.5 rounded-[var(--radius-full)] border px-3 py-1.5 text-[12px] font-medium transition-colors',
              filterPinned
                ? 'border-[var(--accent)] bg-[var(--accent-light)] text-[var(--accent)]'
                : 'border-[var(--border)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
            )}
          >
            <Pin className="h-3 w-3" /> Fixadas {pinnedCount > 0 ? `(${pinnedCount})` : ''}
          </button>
          {(['recent', 'name', 'size'] as SortKey[]).map((key) => (
            <button
              key={key}
              onClick={() => setSort(key)}
              className={cn(
                'rounded-[var(--radius-full)] border px-3 py-1.5 text-[12px] font-medium transition-colors',
                sort === key
                  ? 'border-[var(--accent)] bg-[var(--accent-light)] text-[var(--accent)]'
                  : 'border-[var(--border)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
              )}
            >
              {key === 'recent' ? 'Recente' : key === 'name' ? 'Nome' : 'Tamanho'}
            </button>
          ))}
        </div>

        {/* Results */}
        {filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-[var(--radius-lg)] border border-dashed border-[var(--border)] bg-[var(--surface)] px-6 py-16 text-center">
            <BookOpen className="h-10 w-10 text-[var(--subtle-foreground)]" />
            <p className="mt-3 text-[16px] font-medium text-[var(--foreground)]">
              {query ? 'Nenhum resultado' : 'Nenhuma conversa ainda'}
            </p>
            <p className="mt-1 text-[14px] text-[var(--muted-foreground)]">
              {query ? 'Refine a busca.' : 'Inicie uma conversa no chat.'}
            </p>
          </div>
        ) : view === 'grid' ? (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {filtered.map((c) => (
              <article
                key={c.id}
                className="group rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-4"
              >
                <div className="flex items-start justify-between">
                  <span className="flex h-9 w-9 items-center justify-center rounded-full bg-[var(--surface-hover)] text-[12px] font-semibold text-[var(--muted-foreground)]">
                    {c.title.slice(0, 2).toUpperCase()}
                  </span>
                  {c.pinned && <Pin className="h-3.5 w-3.5 text-[var(--warning)]" />}
                </div>
                <h3 className="mt-3 truncate text-[14px] font-medium text-[var(--foreground)]">{c.title}</h3>
                <p className="mt-1 text-[12px] text-[var(--muted-foreground)]">
                  {c.model} · {c.messages.length} msgs · {new Date(c.updatedAt).toLocaleDateString('pt-BR')}
                </p>
                <div className="mt-3 flex gap-2">
                  <button
                    onClick={() => openConversation(c.id)}
                    className="flex-1 rounded-[var(--radius-md)] bg-[var(--primary)] px-3 py-2 text-[13px] font-medium text-[var(--primary-foreground)] hover:opacity-90"
                  >
                    Abrir
                  </button>
                  <button
                    onClick={() => toggleConversationPinned(c.id)}
                    className="rounded-[var(--radius-md)] border border-[var(--border)] p-2 text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                  >
                    <Pin className="h-3.5 w-3.5" />
                  </button>
                  <button
                    onClick={() => removeConversation(c.id)}
                    className="rounded-[var(--radius-md)] border border-[var(--border)] p-2 text-[var(--muted-foreground)] hover:text-[var(--destructive)]"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="space-y-2">
            {filtered.map((c) => (
              <article
                key={c.id}
                className="group flex items-center gap-4 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-4 py-3"
              >
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--surface-hover)] text-[12px] font-semibold text-[var(--muted-foreground)]">
                  {c.title.slice(0, 2).toUpperCase()}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="truncate text-[14px] font-medium text-[var(--foreground)]">{c.title}</h3>
                    {c.pinned && <Pin className="h-3 w-3 shrink-0 text-[var(--warning)]" />}
                  </div>
                  <div className="mt-0.5 flex gap-3 text-[12px] text-[var(--muted-foreground)]">
                    <span className="inline-flex items-center gap-1"><MessageSquare className="h-3 w-3" /> {c.messages.length}</span>
                    <span className="inline-flex items-center gap-1"><Clock3 className="h-3 w-3" /> {new Date(c.updatedAt).toLocaleDateString('pt-BR')}</span>
                    <span>{c.model}</span>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => toggleConversationPinned(c.id)}
                    className="rounded-[var(--radius-md)] p-2 text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]"
                  >
                    <Pin className="h-3.5 w-3.5" />
                  </button>
                  <button
                    onClick={() => removeConversation(c.id)}
                    className="rounded-[var(--radius-md)] p-2 text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--destructive)]"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                  <button
                    onClick={() => openConversation(c.id)}
                    className="inline-flex items-center gap-1.5 rounded-[var(--radius-md)] bg-[var(--primary)] px-3 py-1.5 text-[13px] font-medium text-[var(--primary-foreground)] hover:opacity-90"
                  >
                    Abrir <ChevronRight className="h-3 w-3" />
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </AppLayout>
  );
}
