'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MessageSquare, BookOpen, Zap, CreditCard, Settings, Plus, Pin, ArrowRight } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { cn } from '@/lib/cn';
import { useChatStore } from '@/stores/chat-store';
import { useTranslations } from 'next-intl';

export interface CommandItem {
  id: string;
  label: string;
  description?: string;
  icon: React.ReactNode;
  group: string;
  action: () => void;
  keywords?: string[];
}

interface CommandPaletteProps {
  open: boolean;
  onClose: () => void;
}

const FREQUENCY_STORAGE_KEY = 'lume-command-palette-frequency';

function bumpCommandUsage(commandId: string) {
  if (typeof window === 'undefined') return;
  const raw = localStorage.getItem(FREQUENCY_STORAGE_KEY);
  const parsed = raw ? (JSON.parse(raw) as Record<string, number>) : {};
  parsed[commandId] = (parsed[commandId] || 0) + 1;
  localStorage.setItem(FREQUENCY_STORAGE_KEY, JSON.stringify(parsed));
}

function readCommandUsage() {
  if (typeof window === 'undefined') return {} as Record<string, number>;
  const raw = localStorage.getItem(FREQUENCY_STORAGE_KEY);
  return raw ? (JSON.parse(raw) as Record<string, number>) : {};
}

function useCommandItems(onClose: () => void): CommandItem[] {
  const t = useTranslations();
  const router = useRouter();
  const { conversations, createConversation, setActiveConversation } = useChatStore();

  const action = (id: string, run: () => void) => () => {
    bumpCommandUsage(id);
    run();
  };
  const navigate = (path: string) => { router.push(path); onClose(); };
  const openConversation = (id: string) => { setActiveConversation(id); router.push('/chat'); onClose(); };
  const newConversation = () => { createConversation(); router.push('/chat'); onClose(); };

  const baseItems: CommandItem[] = [
    { id: 'nav-chat', label: t('commandPalette.actions.goChat'), icon: <MessageSquare className="h-4 w-4" />, group: t('commandPalette.groups.navigate'), action: action('nav-chat', () => navigate('/chat')), keywords: ['chat', 'conversa'] },
    { id: 'nav-library', label: t('commandPalette.actions.goLibrary'), icon: <BookOpen className="h-4 w-4" />, group: t('commandPalette.groups.navigate'), action: action('nav-library', () => navigate('/library')), keywords: ['biblioteca', 'acervo'] },
    { id: 'nav-prompts', label: t('commandPalette.actions.goPrompts'), icon: <Zap className="h-4 w-4" />, group: t('commandPalette.groups.navigate'), action: action('nav-prompts', () => navigate('/prompts')), keywords: ['template', 'prompt'] },
    { id: 'nav-billing', label: t('commandPalette.actions.goBilling'), icon: <CreditCard className="h-4 w-4" />, group: t('commandPalette.groups.navigate'), action: action('nav-billing', () => navigate('/billing')), keywords: ['plano', 'billing'] },
    { id: 'nav-settings', label: t('commandPalette.actions.goSettings'), icon: <Settings className="h-4 w-4" />, group: t('commandPalette.groups.navigate'), action: action('nav-settings', () => navigate('/settings')), keywords: ['configuracoes', 'perfil'] },
    { id: 'action-new-conv', label: t('commandPalette.actions.newConversation'), description: t('commandPalette.descriptions.newConversation'), icon: <Plus className="h-4 w-4" />, group: t('commandPalette.groups.actions'), action: action('action-new-conv', newConversation), keywords: ['nova', 'criar'] },
  ];

  const frequency = readCommandUsage();
  const frequentItems = [...baseItems]
    .sort((a, b) => (frequency[b.id] || 0) - (frequency[a.id] || 0))
    .filter((item) => (frequency[item.id] || 0) > 0)
    .slice(0, 3)
    .map((item) => ({ ...item, group: t('commandPalette.groups.frequent') }));

  return [
    ...frequentItems,
    ...baseItems,
    ...conversations.slice(0, 8).map((conversation) => ({
      id: `conv-${conversation.id}`,
      label: conversation.title,
      description: `${conversation.model} · ${conversation.messages.length} mensagens`,
      icon: conversation.pinned ? <Pin className="h-4 w-4" /> : <MessageSquare className="h-4 w-4" />,
      group: t('commandPalette.groups.recentConversations'),
      action: () => openConversation(conversation.id),
      keywords: [conversation.title.toLowerCase(), conversation.model],
    })),
  ];
}

export function CommandPalette({ open, onClose }: CommandPaletteProps) {
  const t = useTranslations();
  const [query, setQuery] = useState('');
  const [highlighted, setHighlighted] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const items = useCommandItems(onClose);

  const filtered = query.trim()
    ? items.filter((item) => {
        const normalizedQuery = query.toLowerCase();
        return item.label.toLowerCase().includes(normalizedQuery)
          || item.description?.toLowerCase().includes(normalizedQuery)
          || item.keywords?.some((keyword) => keyword.includes(normalizedQuery));
      })
    : items;

  const groups = filtered.reduce<Record<string, CommandItem[]>>((accumulator, item) => {
    if (!accumulator[item.group]) accumulator[item.group] = [];
    accumulator[item.group].push(item);
    return accumulator;
  }, {});

  useEffect(() => {
    if (!open) return;
    setQuery('');
    setHighlighted(0);
    setTimeout(() => inputRef.current?.focus(), 50);
  }, [open]);

  const handleKeyDown = useCallback((event: React.KeyboardEvent) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setHighlighted((current) => Math.min(current + 1, filtered.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlighted((current) => Math.max(current - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      filtered[highlighted]?.action();
    } else if (event.key === 'Escape') {
      onClose();
    }
  }, [filtered, highlighted, onClose]);

  if (typeof document === 'undefined') return null;

  return createPortal(
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-[var(--z-modal)] flex items-start justify-center px-4 pt-[12vh]">
          <motion.div
            className="absolute inset-0 bg-[rgba(3,8,18,0.72)] backdrop-blur-md"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            onClick={onClose}
          />
          <motion.div
            className="glass relative z-10 w-full max-w-2xl overflow-hidden rounded-[var(--radius-xl)] shadow-[var(--shadow-xl)]"
            initial={{ opacity: 0, scale: 0.96, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: -10 }}
            transition={{ duration: 0.18, ease: [0.34, 1.56, 0.64, 1] }}
            onKeyDown={handleKeyDown}
          >
            <div className="flex items-center gap-3 border-b border-[var(--glass-border)] px-5 py-4">
              <Search className="h-4 w-4 shrink-0 text-[var(--muted-foreground)]" />
              <input
                ref={inputRef}
                value={query}
                onChange={(event) => { setQuery(event.target.value); setHighlighted(0); }}
                placeholder={t('commandPalette.searchPlaceholder')}
                className="flex-1 bg-transparent text-[var(--text-sm)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus:outline-none"
                aria-label={t('commandPalette.searchCommands')}
                autoComplete="off"
              />
              <kbd className="rounded-full border border-[var(--border)] bg-[rgba(255,255,255,0.04)] px-2 py-1 text-[0.62rem] text-[var(--muted-foreground)]">ESC</kbd>
            </div>
            <div className="max-h-[28rem] overflow-y-auto py-3">
              {filtered.length === 0 ? (
                <div className="px-5 py-10 text-center text-[var(--text-sm)] text-[var(--muted-foreground)]">
                  {t('commandPalette.empty', { query })}
                </div>
              ) : (
                Object.entries(groups).map(([group, groupItems]) => {
                  const offset = filtered.findIndex((item) => item.id === groupItems[0].id);
                  return (
                    <div key={group}>
                      <div className="px-5 py-1.5 text-[0.64rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">
                        {group}
                      </div>
                      {groupItems.map((item, index) => {
                        const flatIndex = offset + index;
                        return (
                          <button
                            key={item.id}
                            type="button"
                            onMouseEnter={() => setHighlighted(flatIndex)}
                            onClick={item.action}
                            className={cn(
                              'flex w-full items-center gap-3 px-5 py-3 text-left transition-colors',
                              flatIndex === highlighted ? 'bg-[rgba(96,115,255,0.12)] text-[var(--foreground)]' : 'text-[var(--foreground)]'
                            )}
                          >
                            <span className={cn('shrink-0', flatIndex === highlighted ? 'text-[var(--brand-primary)]' : 'text-[var(--muted-foreground)]')}>
                              {item.icon}
                            </span>
                            <span className="min-w-0 flex-1">
                              <span className="block truncate text-[var(--text-sm)] font-medium">{item.label}</span>
                              {item.description && <span className="block truncate text-[0.7rem] text-[var(--muted-foreground)]">{item.description}</span>}
                            </span>
                            {flatIndex === highlighted && <ArrowRight className="h-3.5 w-3.5 shrink-0 text-[var(--brand-primary)]" />}
                          </button>
                        );
                      })}
                    </div>
                  );
                })
              )}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    document.body
  );
}

export function useCommandPalette() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        setOpen((current) => !current);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  return { open, setOpen };
}
