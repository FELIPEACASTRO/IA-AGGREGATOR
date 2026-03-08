'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import {
  ArrowRight,
  BookOpen,
  CreditCard,
  FolderOpen,
  MessageSquare,
  Search,
  Settings,
  Sparkles,
  TerminalSquare,
  Wrench,
} from 'lucide-react';
import { SearchField } from '@/components/ui/search-field';
import type { SearchResponse, SearchResultItem } from '@/lib/contracts/platform';
import { cn } from '@/lib/cn';
import { useChatStore } from '@/stores/chat-store';

export type CommandPaletteItem = {
  id: string;
  label: string;
  href?: string;
  action?: () => void;
  subtitle?: string;
  icon?: React.ReactNode;
  keywords?: string[];
};

export type CommandPaletteSection = {
  id: string;
  title: string;
  items: CommandPaletteItem[];
};

type CommandPaletteProps = {
  scope: 'consumer' | 'codex';
  quickActions: CommandPaletteItem[];
};

const HISTORY_LIMIT = 6;

function getHistoryKey(scope: CommandPaletteProps['scope']) {
  return `lume-command-history:${scope}`;
}

function itemMatchesQuery(item: CommandPaletteItem, query: string) {
  if (!query) return true;
  const haystack = `${item.label} ${item.subtitle ?? ''} ${(item.keywords ?? []).join(' ')}`.toLowerCase();
  return haystack.includes(query.toLowerCase());
}

function mapSearchItem(item: SearchResultItem): CommandPaletteItem {
  return {
    id: `search-${item.id}`,
    label: item.title,
    subtitle: item.description,
    href: item.href,
    icon: <Search className="h-4 w-4" />,
    keywords: [item.kind],
  };
}

export function CommandPalette({ scope, quickActions }: CommandPaletteProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<CommandPaletteItem[]>([]);
  const [history, setHistory] = useState<CommandPaletteItem[]>([]);
  const [activeIndex, setActiveIndex] = useState(0);
  const router = useRouter();
  const pathname = usePathname();
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const rawConversations = useChatStore((state) => state.conversations);
  const conversations = useMemo(
    () =>
      Array.isArray(rawConversations)
        ? rawConversations
        : Array.isArray((rawConversations as { conversations?: unknown[] } | undefined)?.conversations)
          ? ((rawConversations as { conversations: typeof rawConversations }).conversations as typeof rawConversations)
          : [],
    [rawConversations],
  );

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const raw = window.localStorage.getItem(getHistoryKey(scope));
    if (!raw) return;

    try {
      const parsed = JSON.parse(raw) as CommandPaletteItem[];
      setHistory(parsed);
    } catch {
      setHistory([]);
    }
  }, [scope]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        setOpen((current) => !current);
      }

      if (event.key === 'Escape') {
        setOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  useEffect(() => {
    const handleOpen = (event: Event) => {
      const customEvent = event as CustomEvent<{ scope?: CommandPaletteProps['scope'] }>;
      if (customEvent.detail?.scope && customEvent.detail.scope !== scope) return;
      setOpen(true);
    };

    window.addEventListener('lume-command-palette:open', handleOpen as EventListener);
    return () => window.removeEventListener('lume-command-palette:open', handleOpen as EventListener);
  }, [scope]);

  useEffect(() => {
    if (!open) {
      setQuery('');
      setResults([]);
      setActiveIndex(0);
      return;
    }

    if (searchTimer.current) clearTimeout(searchTimer.current);

    if (query.trim().length < 2) {
      setResults([]);
      return;
    }

    searchTimer.current = setTimeout(async () => {
      try {
        const response = await fetch(`/api/v1/search?q=${encodeURIComponent(query.trim())}`, {
          cache: 'no-store',
        });
        const payload = (await response.json()) as { success?: boolean; data?: SearchResponse };
        if (!response.ok || !payload.data) {
          setResults([]);
          return;
        }

        setResults(payload.data.items.map(mapSearchItem));
      } catch {
        setResults([]);
      }
    }, 180);

    return () => {
      if (searchTimer.current) clearTimeout(searchTimer.current);
    };
  }, [open, query]);

  useEffect(() => {
    if (!open) return;
    setActiveIndex(0);
  }, [open, query]);

  const recentConversations = useMemo<CommandPaletteItem[]>(
    () =>
      scope === 'consumer'
        ? conversations.slice(0, HISTORY_LIMIT).map((conversation) => ({
            id: `conversation-${conversation.id}`,
            label: conversation.title,
            subtitle: `${conversation.messages.length} mensagens`,
            href: '/chat',
            icon: <MessageSquare className="h-4 w-4" />,
            keywords: [conversation.model],
          }))
        : [],
    [conversations, scope],
  );

  const quickSectionItems = useMemo(
    () => quickActions.filter((item) => itemMatchesQuery(item, query)),
    [quickActions, query],
  );

  const historyItems = useMemo(
    () => history.filter((item) => itemMatchesQuery(item, query)),
    [history, query],
  );

  const recents = useMemo(
    () => [...historyItems, ...recentConversations].slice(0, HISTORY_LIMIT),
    [historyItems, recentConversations],
  );

  const sections = useMemo<CommandPaletteSection[]>(
    () => [
      { id: 'quick', title: 'Acoes rapidas', items: quickSectionItems },
      { id: 'recent', title: 'Recentes', items: recents },
      { id: 'results', title: 'Resultados', items: results },
    ].filter((section) => section.items.length > 0),
    [quickSectionItems, recents, results],
  );

  const flattened = useMemo(
    () => sections.flatMap((section) => section.items),
    [sections],
  );

  const persistHistory = useCallback((item: CommandPaletteItem) => {
    const next = [
      {
        id: item.id,
        label: item.label,
        href: item.href,
        subtitle: item.subtitle,
        keywords: item.keywords,
      },
      ...history.filter((entry) => entry.id !== item.id),
    ].slice(0, HISTORY_LIMIT);

    setHistory(next);

    if (typeof window !== 'undefined') {
      window.localStorage.setItem(getHistoryKey(scope), JSON.stringify(next));
    }
  }, [history, scope]);

  const selectItem = useCallback((item: CommandPaletteItem) => {
    persistHistory(item);
    setOpen(false);

    if (item.action) {
      item.action();
      return;
    }

    if (item.href && item.href !== pathname) {
      router.push(item.href);
    }
  }, [pathname, persistHistory, router]);

  useEffect(() => {
    if (!open) return;

    const onKeyDown = (event: KeyboardEvent) => {
      if (!flattened.length) return;

      if (event.key === 'ArrowDown') {
        event.preventDefault();
        setActiveIndex((current) => (current + 1) % flattened.length);
      }

      if (event.key === 'ArrowUp') {
        event.preventDefault();
        setActiveIndex((current) => (current - 1 + flattened.length) % flattened.length);
      }

      if (event.key === 'Enter') {
        event.preventDefault();
        const current = flattened[activeIndex];
        if (current) selectItem(current);
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, activeIndex, flattened, selectItem]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[var(--z-modal)] bg-black/35 px-4 py-20 backdrop-blur-[2px]">
      <div
        className="mx-auto max-w-2xl overflow-hidden rounded-[var(--radius-lg)] border border-[var(--border-strong)] shadow-[var(--shadow-lg)]"
        style={{ backgroundColor: 'rgba(31, 30, 29, 0.94)' }}
      >
        <div className="border-b border-[var(--border)] p-3">
          <SearchField
            value={query}
            onChange={setQuery}
            placeholder="Buscar ou executar uma acao..."
            className="w-full"
            inputClassName="bg-[var(--bg-000)]"
          />
        </div>

        <div className="max-h-[420px] overflow-y-auto p-2">
          {sections.map((section) => (
            <div key={section.id} className="pb-2">
              <p className="px-2 py-2 text-[11px] uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">
                {section.title}
              </p>
              <div className="space-y-1">
                {section.items.map((item) => {
                  const index = flattened.findIndex((candidate) => candidate.id === item.id);
                  const active = index === activeIndex;

                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => selectItem(item)}
                      onMouseEnter={() => setActiveIndex(index)}
                      className={cn(
                        'flex w-full items-center gap-3 rounded-[var(--radius-md)] px-3 py-2 text-left transition-colors',
                        active
                          ? 'bg-[var(--surface-hover)] text-[var(--foreground)]'
                          : 'text-[var(--text-200)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]',
                      )}
                    >
                      <span className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-[var(--radius-md)] bg-[var(--surface)] text-[var(--accent-brand)]">
                        {item.icon ?? <Sparkles className="h-4 w-4" />}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-[13px] font-medium">{item.label}</span>
                        {item.subtitle ? (
                          <span className="block truncate text-[11px] text-[var(--subtle-foreground)]">
                            {item.subtitle}
                          </span>
                        ) : null}
                      </span>
                      <ArrowRight className="h-4 w-4 shrink-0 text-[var(--subtle-foreground)]" />
                    </button>
                  );
                })}
              </div>
            </div>
          ))}

          {sections.length === 0 ? (
            <div className="px-4 py-10 text-center text-[13px] text-[var(--subtle-foreground)]">
              Nenhum resultado encontrado.
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export const consumerQuickActions: CommandPaletteItem[] = [
  {
    id: 'consumer-chat',
    label: 'Abrir Chat',
    href: '/chat',
    subtitle: 'Conversa principal com IA',
    icon: <MessageSquare className="h-4 w-4" />,
    keywords: ['conversa', 'mensagem'],
  },
  {
    id: 'consumer-library',
    label: 'Abrir Biblioteca',
    href: '/library',
    subtitle: 'Conversas salvas e fixadas',
    icon: <BookOpen className="h-4 w-4" />,
    keywords: ['biblioteca', 'historico'],
  },
  {
    id: 'consumer-prompts',
    label: 'Abrir Templates',
    href: '/prompts',
    subtitle: 'Prompts curados',
    icon: <Sparkles className="h-4 w-4" />,
    keywords: ['template', 'prompt'],
  },
  {
    id: 'consumer-settings',
    label: 'Abrir Configuracoes',
    href: '/settings',
    subtitle: 'Tema, idioma e preferencias',
    icon: <Settings className="h-4 w-4" />,
    keywords: ['ajustes', 'tema'],
  },
  {
    id: 'consumer-billing',
    label: 'Abrir Billing',
    href: '/billing',
    subtitle: 'Uso e plano atual',
    icon: <CreditCard className="h-4 w-4" />,
    keywords: ['plano', 'uso'],
  },
];

export const codexQuickActions: CommandPaletteItem[] = [
  {
    id: 'codex-home',
    label: 'Abrir Dashboard Codex',
    href: '/codex',
    subtitle: 'Visao geral de tasks',
    icon: <TerminalSquare className="h-4 w-4" />,
    keywords: ['dashboard', 'tasks'],
  },
  {
    id: 'codex-env',
    label: 'Ambientes',
    href: '/codex/settings/environments',
    subtitle: 'Runtime e politicas',
    icon: <Wrench className="h-4 w-4" />,
    keywords: ['environment', 'setup'],
  },
  {
    id: 'codex-connectors',
    label: 'Connectors',
    href: '/codex/settings/connectors',
    subtitle: 'Integracoes e permisos',
    icon: <FolderOpen className="h-4 w-4" />,
    keywords: ['slack', 'github', 'linear'],
  },
  {
    id: 'codex-usage',
    label: 'Usage',
    href: '/codex/settings/usage',
    subtitle: 'Consumo e creditos',
    icon: <CreditCard className="h-4 w-4" />,
    keywords: ['creditos', 'usage'],
  },
];

export function openCommandPalette(scope?: CommandPaletteProps['scope']) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent('lume-command-palette:open', { detail: { scope } }));
}
