'use client';

import { AppShell } from '@/components/app/app-shell';
import { useEffect, useMemo, useState } from 'react';
import { useChatStore } from '@/stores/chat-store';
import { useRouter } from 'next/navigation';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/cn';
import {
  BookOpen, Clock3, MessageSquare, Pin, Search,
  Trash2, ChevronRight, LayoutGrid, List, ArrowUpDown,
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

  const filtered = useMemo(() => {
    let list = conversations.filter((c) =>
      c.title.toLowerCase().includes(query.toLowerCase())
    );
    if (filterPinned) list = list.filter((c) => c.pinned);
    if (sort === 'name') list = [...list].sort((a, b) => a.title.localeCompare(b.title));
    else if (sort === 'size') list = [...list].sort((a, b) => b.messages.length - a.messages.length);
    else list = [...list].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
    return list;
  }, [conversations, query, sort, filterPinned]);

  const pinnedCount = conversations.filter((c) => c.pinned).length;

  return (
    <AppShell
      title="Biblioteca"
      subtitle={`${conversations.length} conversa${conversations.length !== 1 ? 's' : ''} salvas`}
      headerActions={
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-[var(--muted-foreground)]" />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Buscar..."
              className="h-8 w-52 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] pl-8 pr-3 text-[var(--text-xs)] placeholder:text-[var(--muted-foreground)] focus:outline-none focus:ring-1 focus:ring-[var(--ring)]" />
          </div>
          <div className="flex rounded-[var(--radius-md)] border border-[var(--border)] overflow-hidden">
            <button onClick={() => setView('list')} className={cn('p-1.5 transition-colors', view === 'list' ? 'bg-[var(--brand-primary)] text-white' : 'bg-[var(--surface-1)] text-[var(--muted-foreground)] hover:bg-[var(--surface-2)]')}>
              <List className="h-3.5 w-3.5" />
            </button>
            <button onClick={() => setView('grid')} className={cn('p-1.5 transition-colors', view === 'grid' ? 'bg-[var(--brand-primary)] text-white' : 'bg-[var(--surface-1)] text-[var(--muted-foreground)] hover:bg-[var(--surface-2)]')}>
              <LayoutGrid className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      }
    >
      <div className="py-6 space-y-5">
        {/* Filters */}
        <div className="flex flex-wrap items-center gap-2">
          <button onClick={() => setFilterPinned((v) => !v)}
            className={cn('inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] border px-3 py-1.5 text-[var(--text-xs)] font-medium transition-colors',
              filterPinned ? 'border-[var(--brand-primary)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]' : 'border-[var(--border)] bg-[var(--surface-1)] text-[var(--muted-foreground)] hover:border-[var(--border-strong)]')}>
            <Pin className="h-3 w-3" /> Fixadas {pinnedCount > 0 && `(${pinnedCount})`}
          </button>
          <div className="flex items-center gap-1.5 ml-auto">
            <ArrowUpDown className="h-3.5 w-3.5 text-[var(--muted-foreground)]" />
            {(['recent', 'name', 'size'] as SortKey[]).map((key) => (
              <button key={key} onClick={() => setSort(key)}
                className={cn('rounded-[var(--radius-pill)] border px-2.5 py-1 text-[var(--text-xs)] transition-colors',
                  sort === key ? 'border-[var(--brand-primary)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]' : 'border-[var(--border)] bg-[var(--surface-1)] text-[var(--muted-foreground)] hover:border-[var(--border-strong)]')}>
                {key === 'recent' ? 'Recente' : key === 'name' ? 'Nome' : 'Tamanho'}
              </button>
            ))}
          </div>
        </div>

        {filtered.length === 0 ? (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}
            className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] p-12 text-center">
            <BookOpen className="mx-auto h-10 w-10 text-[var(--muted-foreground)] opacity-40 mb-3" />
            <p className="font-medium text-[var(--text-sm)]">
              {query ? 'Nenhum resultado encontrado' : 'Nenhuma conversa ainda'}
            </p>
            <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">
              {query ? 'Tente outro termo de busca.' : 'Inicie uma conversa no chat para ela aparecer aqui.'}
            </p>
          </motion.div>
        ) : view === 'grid' ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <AnimatePresence>
              {filtered.map((conv, i) => (
                <motion.article key={conv.id} initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }}
                  transition={{ duration: 0.2, delay: i * 0.03 }}
                  className="group rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] p-4 hover:border-[var(--border-strong)] hover:shadow-[var(--shadow-md)] transition-all">
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-[var(--radius-md)] bg-[var(--brand-primary)]/10 text-[var(--text-xs)] font-bold text-[var(--brand-primary)]">
                      {conv.title.slice(0, 2).toUpperCase()}
                    </div>
                    {conv.pinned && <Pin className="h-3.5 w-3.5 text-[var(--warning)] shrink-0" />}
                  </div>
                  <h3 className="truncate text-[var(--text-sm)] font-semibold mb-1">{conv.title}</h3>
                  <p className="text-[10px] text-[var(--muted-foreground)] mb-1">{conv.model}</p>
                  <p className="text-[10px] text-[var(--subtle-foreground)] flex items-center gap-1">
                    <MessageSquare className="h-3 w-3" /> {conv.messages.length} msg{conv.messages.length !== 1 ? 's' : ''}
                  </p>
                  <div className="mt-3 flex gap-1.5">
                    <button onClick={() => { setActiveConversation(conv.id); trackEvent('library_open_conversation', { conversationId: conv.id }); router.push('/chat'); toast.success('Conversa aberta'); }}
                      className="flex-1 rounded-[var(--radius-md)] bg-[var(--brand-primary)] py-1.5 text-[var(--text-xs)] font-medium text-white hover:opacity-90 transition-opacity">
                      Abrir
                    </button>
                    <button onClick={() => { toggleConversationPinned(conv.id); trackEvent('library_toggle_pin', { conversationId: conv.id }); }}
                      className="rounded-[var(--radius-md)] border border-[var(--border)] p-1.5 text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] transition-colors">
                      <Pin className="h-3.5 w-3.5" />
                    </button>
                    <button onClick={() => { if (window.confirm('Excluir esta conversa?')) { deleteConversation(conv.id); toast.success('Excluída'); }}}
                      className="rounded-[var(--radius-md)] border border-[var(--border)] p-1.5 text-[var(--muted-foreground)] hover:bg-[var(--destructive)]/10 hover:text-[var(--destructive)] hover:border-[var(--destructive)]/30 transition-colors">
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </motion.article>
              ))}
            </AnimatePresence>
          </div>
        ) : (
          <div className="space-y-1.5">
            <AnimatePresence>
              {filtered.map((conv, i) => (
                <motion.article key={conv.id} initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.2, delay: i * 0.02 }}
                  className="group flex items-center gap-3 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-1)] px-4 py-3 hover:border-[var(--border-strong)] hover:bg-[var(--surface-2)] transition-all">
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--surface-2)] text-[10px] font-bold text-[var(--muted-foreground)] group-hover:bg-[var(--brand-primary)]/10 group-hover:text-[var(--brand-primary)] transition-colors">
                    {conv.title.slice(0, 2).toUpperCase()}
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="truncate text-[var(--text-sm)] font-medium">{conv.title}</h3>
                      {conv.pinned && <Pin className="h-3 w-3 shrink-0 text-[var(--warning)]" />}
                    </div>
                    <p className="text-[10px] text-[var(--subtle-foreground)] flex items-center gap-2 mt-0.5">
                      <span>{conv.model}</span>
                      <span>·</span>
                      <MessageSquare className="h-2.5 w-2.5" /><span>{conv.messages.length} msgs</span>
                      <span>·</span>
                      <Clock3 className="h-2.5 w-2.5" /><span>{new Date(conv.updatedAt).toLocaleDateString('pt-BR')}</span>
                    </p>
                  </div>
                  <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button onClick={() => { toggleConversationPinned(conv.id); trackEvent('library_toggle_pin', { conversationId: conv.id }); }}
                      className="rounded-[var(--radius-sm)] p-1.5 text-[var(--muted-foreground)] hover:bg-[var(--surface-3)] transition-colors" title={conv.pinned ? 'Desafixar' : 'Fixar'}>
                      <Pin className="h-3.5 w-3.5" />
                    </button>
                    <button onClick={() => { if (window.confirm('Excluir?')) { deleteConversation(conv.id); toast.success('Excluída'); }}}
                      className="rounded-[var(--radius-sm)] p-1.5 text-[var(--muted-foreground)] hover:bg-[var(--destructive)]/10 hover:text-[var(--destructive)] transition-colors" title="Excluir">
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                    <button onClick={() => { setActiveConversation(conv.id); trackEvent('library_open_conversation', { conversationId: conv.id }); router.push('/chat'); toast.success('Conversa aberta'); }}
                      className="flex items-center gap-1.5 rounded-[var(--radius-md)] bg-[var(--brand-primary)] px-3 py-1.5 text-[var(--text-xs)] font-medium text-white hover:opacity-90 transition-opacity">
                      Abrir <ChevronRight className="h-3 w-3" />
                    </button>
                  </div>
                </motion.article>
              ))}
            </AnimatePresence>
          </div>
        )}
      </div>
    </AppShell>
  );
}
