'use client';

import { AppShell } from '@/components/app/app-shell';
import { useMemo, useState } from 'react';
import { useChatStore } from '@/stores/chat-store';
import { useRouter } from 'next/navigation';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/cn';
import { PageSection, PageSplit, PageStack } from '@/components/app/page-blueprint';
import {
  ArrowUpDown,
  BookOpen,
  BrainCircuit,
  ChevronRight,
  Clock3,
  FolderKanban,
  LayoutGrid,
  List,
  MessageSquare,
  Pin,
  Search,
  Sparkles,
  Trash2,
} from 'lucide-react';

type SortKey = 'recent' | 'name' | 'size';
type ViewMode = 'grid' | 'list';

function StatCard({ label, value, helper, icon: Icon }: { label: string; value: string; helper: string; icon: React.ElementType }) {
  return (
    <div className="lume-panel-soft rounded-[var(--radius-xl)] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">{label}</p>
          <p className="mt-2 text-[var(--text-2xl)] font-semibold text-[var(--foreground)]">{value}</p>
          <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{helper}</p>
        </div>
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] text-[var(--brand-primary)]">
          <Icon className="h-5 w-5" />
        </span>
      </div>
    </div>
  );
}

export default function LibraryPage() {
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState<SortKey>('recent');
  const [view, setView] = useState<ViewMode>('list');
  const [filterPinned, setFilterPinned] = useState(false);
  const router = useRouter();
  const { conversations, setActiveConversation, toggleConversationPinned, deleteConversation } = useChatStore();

  const totalMessages = useMemo(
    () => conversations.reduce((sum, conversation) => sum + conversation.messages.length, 0),
    [conversations],
  );

  const pinnedCount = useMemo(
    () => conversations.filter((conversation) => conversation.pinned).length,
    [conversations],
  );

  const modelsInUse = useMemo(
    () => new Set(conversations.map((conversation) => conversation.model)).size,
    [conversations],
  );

  const recentConversation = useMemo(
    () => [...conversations].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())[0],
    [conversations],
  );

  const filtered = useMemo(() => {
    let list = conversations.filter((conversation) =>
      conversation.title.toLowerCase().includes(query.toLowerCase()),
    );

    if (filterPinned) list = list.filter((conversation) => conversation.pinned);

    if (sort === 'name') {
      list = [...list].sort((a, b) => a.title.localeCompare(b.title));
    } else if (sort === 'size') {
      list = [...list].sort((a, b) => b.messages.length - a.messages.length);
    } else {
      list = [...list].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
    }

    return list;
  }, [conversations, filterPinned, query, sort]);

  const openConversation = (conversationId: string) => {
    setActiveConversation(conversationId);
    trackEvent('library_open_conversation', { conversationId });
    router.push('/chat');
    toast.success('Conversa aberta');
  };

  const removeConversation = (conversationId: string) => {
    if (!window.confirm('Excluir esta conversa?')) return;
    deleteConversation(conversationId);
    toast.success('Conversa excluida');
  };

  return (
    <AppShell
      title="Biblioteca"
      subtitle={`${conversations.length} conversa${conversations.length !== 1 ? 's' : ''} salvas e prontas para reuso`}
      headerActions={
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-[var(--muted-foreground)]" />
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Buscar..."
              className="lume-field h-11 w-[min(18rem,50vw)] pl-9"
            />
          </div>
          <div className="flex rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-1">
            <button
              onClick={() => setView('list')}
              className={cn(
                'inline-flex h-9 items-center gap-2 rounded-[var(--radius-pill)] px-3 text-[var(--text-xs)] font-semibold transition-colors',
                view === 'list' ? 'bg-[var(--brand-primary)] text-white shadow-[var(--shadow-brand)]' : 'text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
              )}
              aria-label="Visualizacao em lista"
            >
              <List className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Lista</span>
            </button>
            <button
              onClick={() => setView('grid')}
              className={cn(
                'inline-flex h-9 items-center gap-2 rounded-[var(--radius-pill)] px-3 text-[var(--text-xs)] font-semibold transition-colors',
                view === 'grid' ? 'bg-[var(--brand-primary)] text-white shadow-[var(--shadow-brand)]' : 'text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
              )}
              aria-label="Visualizacao em grade"
            >
              <LayoutGrid className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Grid</span>
            </button>
          </div>
        </div>
      }
    >
      <PageStack>
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <StatCard label="Conversas" value={String(conversations.length)} helper="Biblioteca pronta para reabrir contexto" icon={FolderKanban} />
          <StatCard label="Mensagens" value={totalMessages.toLocaleString('pt-BR')} helper="Volume total salvo no workspace" icon={MessageSquare} />
          <StatCard label="Fixadas" value={String(pinnedCount)} helper="Acessos rapidos para fluxos recorrentes" icon={Pin} />
          <StatCard label="Modelos" value={String(modelsInUse)} helper="Mix de engines usadas no histórico" icon={BrainCircuit} />
        </section>

        <PageSplit
          left={(
            <aside className="space-y-4">
            <PageSection className="p-5">
              <span className="lume-kicker">
                <Sparkles className="h-3.5 w-3.5" /> Workspace memory
              </span>
              <h2 className="mt-4 text-[var(--text-xl)] font-semibold text-[var(--foreground)]">Biblioteca curada para retomar trabalho sem fricção.</h2>
              <p className="mt-3 text-[var(--text-sm)] text-[var(--muted-foreground)]">
                A biblioteca foi desenhada para manter contexto vivo: filtros claros, retomada imediata e histórico com leitura rapida.
              </p>
              {recentConversation ? (
                <div className="mt-5 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
                  <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Mais recente</p>
                  <p className="mt-2 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">Retomar a conversa mais recente</p>
                  <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">
                    Assunto: {recentConversation.title}
                  </p>
                  <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">
                    {recentConversation.model} · {new Date(recentConversation.updatedAt).toLocaleDateString('pt-BR')}
                  </p>
                  <button
                    onClick={() => openConversation(recentConversation.id)}
                    className="mt-4 inline-flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-4 py-2 text-[var(--text-xs)] font-semibold text-[var(--foreground)] hover:border-[var(--brand-primary)]/40 hover:bg-[rgba(96,115,255,0.08)]"
                  >
                    Abrir
                    <ChevronRight className="h-3.5 w-3.5" />
                  </button>
                </div>
              ) : null}
            </PageSection>

            <PageSection variant="soft" className="p-5">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Filtros</p>
                  <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">Selecione recorte e ordem do histórico.</p>
                </div>
                <ArrowUpDown className="h-4 w-4 text-[var(--muted-foreground)]" />
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  onClick={() => setFilterPinned((value) => !value)}
                  className={cn(
                    'inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] border px-3 py-2 text-[var(--text-xs)] font-semibold transition-colors',
                    filterPinned ? 'border-[var(--brand-primary)] bg-[rgba(96,115,255,0.1)] text-[var(--brand-primary)]' : 'border-[var(--border)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
                  )}
                >
                  <Pin className="h-3 w-3" /> Fixadas {pinnedCount > 0 ? `(${pinnedCount})` : ''}
                </button>
                {(['recent', 'name', 'size'] as SortKey[]).map((key) => (
                  <button
                    key={key}
                    onClick={() => setSort(key)}
                    className={cn(
                      'rounded-[var(--radius-pill)] border px-3 py-2 text-[var(--text-xs)] font-semibold transition-colors',
                      sort === key ? 'border-[var(--brand-primary)] bg-[rgba(96,115,255,0.1)] text-[var(--brand-primary)]' : 'border-[var(--border)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
                    )}
                  >
                    {key === 'recent' ? 'Recente' : key === 'name' ? 'Nome' : 'Tamanho'}
                  </button>
                ))}
              </div>
            </PageSection>
            </aside>
          )}
          right={(
            <PageSection className="p-5 md:p-6">
            <div className="flex flex-wrap items-end justify-between gap-3 border-b border-[var(--border)] pb-4">
              <div>
                <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Histórico ativo</p>
                <h2 className="mt-2 text-[var(--text-xl)] font-semibold text-[var(--foreground)]">{filtered.length} resultado{filtered.length !== 1 ? 's' : ''} pronto{filtered.length !== 1 ? 's' : ''} para retomada</h2>
              </div>
              <p className="text-[var(--text-xs)] text-[var(--muted-foreground)]">Alternancia entre lista e grid para priorizar densidade ou exploracao.</p>
            </div>

            {filtered.length === 0 ? (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="flex min-h-[420px] flex-col items-center justify-center rounded-[var(--radius-2xl)] border border-dashed border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-6 text-center"
              >
                <BookOpen className="h-12 w-12 text-[var(--subtle-foreground)]" />
                <p className="mt-4 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">
                  {query ? 'Nenhum resultado encontrado' : 'Nenhuma conversa ainda'}
                </p>
                <p className="mt-2 max-w-md text-[var(--text-sm)] text-[var(--muted-foreground)]">
                  {query ? 'Refine a busca ou troque o filtro para recuperar mais conversas.' : 'Inicie um fluxo no chat e a memoria do workspace passa a aparecer aqui.'}
                </p>
              </motion.div>
            ) : view === 'grid' ? (
              <div className="mt-5 grid gap-4 sm:grid-cols-2 2xl:grid-cols-3">
                <AnimatePresence>
                  {filtered.map((conversation, index) => (
                    <motion.article
                      key={conversation.id}
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -6 }}
                      transition={{ duration: 0.22, delay: index * 0.02 }}
                      className="group rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4 shadow-[var(--shadow-sm)]"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] border border-[var(--border)] bg-[rgba(96,115,255,0.12)] text-[0.78rem] font-bold text-[var(--brand-primary)]">
                          {conversation.title.slice(0, 2).toUpperCase()}
                        </span>
                        {conversation.pinned ? <Pin className="h-4 w-4 text-[var(--warning)]" /> : null}
                      </div>

                      <h3 className="mt-5 truncate text-[var(--text-base)] font-semibold text-[var(--foreground)]">{conversation.title}</h3>
                      <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{conversation.model}</p>

                      <div className="mt-4 grid grid-cols-2 gap-2 text-[var(--text-xs)] text-[var(--muted-foreground)]">
                        <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-2">
                          <span className="block text-[0.68rem] uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Msgs</span>
                          <span className="mt-1 block font-semibold text-[var(--foreground)]">{conversation.messages.length}</span>
                        </div>
                        <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-2">
                          <span className="block text-[0.68rem] uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Update</span>
                          <span className="mt-1 block font-semibold text-[var(--foreground)]">{new Date(conversation.updatedAt).toLocaleDateString('pt-BR')}</span>
                        </div>
                      </div>

                      <div className="mt-4 flex gap-2">
                        <button
                          onClick={() => openConversation(conversation.id)}
                          className="inline-flex flex-1 items-center justify-center gap-2 rounded-[var(--radius-pill)] bg-[var(--brand-primary)] px-4 py-2.5 text-[var(--text-xs)] font-semibold text-white shadow-[var(--shadow-brand)] hover:-translate-y-0.5"
                        >
                          Abrir
                        </button>
                        <button
                          onClick={() => toggleConversationPinned(conversation.id)}
                          aria-label={conversation.pinned ? 'Desafixar' : 'Fixar'}
                          title={conversation.pinned ? 'Desafixar' : 'Fixar'}
                          className="inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-pill)] border border-[var(--border)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                        >
                          <Pin className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => removeConversation(conversation.id)}
                          aria-label="Excluir"
                          title="Excluir"
                          className="inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-pill)] border border-[var(--border)] text-[var(--muted-foreground)] hover:border-[rgba(255,107,135,0.3)] hover:text-[var(--destructive)]"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </motion.article>
                  ))}
                </AnimatePresence>
              </div>
            ) : (
              <div className="mt-5 space-y-3">
                <AnimatePresence>
                  {filtered.map((conversation, index) => (
                    <motion.article
                      key={conversation.id}
                      initial={{ opacity: 0, x: -10 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: 8 }}
                      transition={{ duration: 0.2, delay: index * 0.02 }}
                      className="group flex flex-col gap-4 rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-4 md:flex-row md:items-center"
                    >
                      <div className="flex min-w-0 flex-1 items-start gap-4">
                        <span className="inline-flex h-12 w-12 shrink-0 items-center justify-center rounded-[18px] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] text-[0.78rem] font-bold text-[var(--muted-foreground)] group-hover:text-[var(--brand-primary)]">
                          {conversation.title.slice(0, 2).toUpperCase()}
                        </span>
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-2">
                            <h3 className="truncate text-[var(--text-base)] font-semibold text-[var(--foreground)]">{conversation.title}</h3>
                            {conversation.pinned ? <Pin className="h-3.5 w-3.5 shrink-0 text-[var(--warning)]" /> : null}
                          </div>
                          <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{conversation.model}</p>
                          <div className="mt-3 flex flex-wrap gap-2 text-[0.72rem] text-[var(--subtle-foreground)]">
                            <span className="inline-flex items-center gap-1 rounded-full border border-[var(--border)] px-2.5 py-1">
                              <MessageSquare className="h-3 w-3" /> {conversation.messages.length} msgs
                            </span>
                            <span className="inline-flex items-center gap-1 rounded-full border border-[var(--border)] px-2.5 py-1">
                              <Clock3 className="h-3 w-3" /> {new Date(conversation.updatedAt).toLocaleDateString('pt-BR')}
                            </span>
                          </div>
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center gap-2 md:justify-end">
                        <button
                          onClick={() => toggleConversationPinned(conversation.id)}
                          aria-label={conversation.pinned ? 'Desafixar' : 'Fixar'}
                          title={conversation.pinned ? 'Desafixar' : 'Fixar'}
                          className="inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-pill)] border border-[var(--border)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                        >
                          <Pin className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => removeConversation(conversation.id)}
                          aria-label="Excluir"
                          title="Excluir"
                          className="inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-pill)] border border-[var(--border)] text-[var(--muted-foreground)] hover:border-[rgba(255,107,135,0.3)] hover:text-[var(--destructive)]"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => openConversation(conversation.id)}
                          className="inline-flex items-center gap-2 rounded-[var(--radius-pill)] bg-[var(--brand-primary)] px-4 py-2.5 text-[var(--text-xs)] font-semibold text-white shadow-[var(--shadow-brand)] hover:-translate-y-0.5"
                        >
                          Abrir
                          <ChevronRight className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </motion.article>
                  ))}
                </AnimatePresence>
              </div>
            )}
            </PageSection>
          )}
        />
      </PageStack>
    </AppShell>
  );
}

