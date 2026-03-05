'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MessageSquare, BookOpen, Zap, CreditCard, Settings, Plus, Trash2, Pin, ArrowRight } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { cn } from '@/lib/cn';
import { useChatStore } from '@/stores/chat-store';

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

function useCommandItems(onClose: () => void): CommandItem[] {
  const router = useRouter();
  const { conversations, createConversation, setActiveConversation } = useChatStore();

  const navigate = (path: string) => { router.push(path); onClose(); };
  const openConv = (id: string) => { setActiveConversation(id); router.push('/chat'); onClose(); };
  const newConv = () => { createConversation(); router.push('/chat'); onClose(); };

  return [
    // Navigation
    { id: 'nav-chat', label: 'Ir para Chat', icon: <MessageSquare className="h-4 w-4" />, group: 'Navegar', action: () => navigate('/chat'), keywords: ['chat', 'conversar'] },
    { id: 'nav-library', label: 'Ir para Biblioteca', icon: <BookOpen className="h-4 w-4" />, group: 'Navegar', action: () => navigate('/library'), keywords: ['biblioteca', 'histórico'] },
    { id: 'nav-prompts', label: 'Ir para Prompts', icon: <Zap className="h-4 w-4" />, group: 'Navegar', action: () => navigate('/prompts'), keywords: ['prompts', 'templates'] },
    { id: 'nav-billing', label: 'Ir para Plano', icon: <CreditCard className="h-4 w-4" />, group: 'Navegar', action: () => navigate('/billing'), keywords: ['plano', 'billing', 'crédito'] },
    { id: 'nav-settings', label: 'Ir para Configurações', icon: <Settings className="h-4 w-4" />, group: 'Navegar', action: () => navigate('/settings'), keywords: ['configurações', 'settings'] },
    // Actions
    { id: 'action-new-conv', label: 'Nova Conversa', description: 'Iniciar uma nova conversa', icon: <Plus className="h-4 w-4" />, group: 'Ações', action: newConv, keywords: ['nova', 'new', 'criar'] },
    // Recent conversations
    ...conversations.slice(0, 8).map((conv) => ({
      id: `conv-${conv.id}`,
      label: conv.title,
      description: `${conv.model} · ${conv.messages.length} mensagens`,
      icon: conv.pinned ? <Pin className="h-4 w-4" /> : <MessageSquare className="h-4 w-4" />,
      group: 'Conversas recentes',
      action: () => openConv(conv.id),
      keywords: [conv.title.toLowerCase(), conv.model],
    })),
  ];
}

export function CommandPalette({ open, onClose }: CommandPaletteProps) {
  const [query, setQuery] = useState('');
  const [highlighted, setHighlighted] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const items = useCommandItems(onClose);

  const filtered = query.trim()
    ? items.filter((item) => {
        const q = query.toLowerCase();
        return (
          item.label.toLowerCase().includes(q) ||
          item.description?.toLowerCase().includes(q) ||
          item.keywords?.some((k) => k.includes(q))
        );
      })
    : items;

  // Group by category
  const groups = filtered.reduce<Record<string, CommandItem[]>>((acc, item) => {
    if (!acc[item.group]) acc[item.group] = [];
    acc[item.group].push(item);
    return acc;
  }, {});

  // Flat index for keyboard nav
  const flatItems = filtered;

  useEffect(() => {
    if (open) {
      setQuery('');
      setHighlighted(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [open]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setHighlighted((h) => Math.min(h + 1, flatItems.length - 1));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setHighlighted((h) => Math.max(h - 1, 0));
      } else if (e.key === 'Enter') {
        e.preventDefault();
        flatItems[highlighted]?.action();
      } else if (e.key === 'Escape') {
        onClose();
      }
    },
    [flatItems, highlighted, onClose]
  );

  if (typeof document === 'undefined') return null;

  return createPortal(
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-[var(--z-modal)] flex items-start justify-center pt-[15vh] px-4">
          {/* Backdrop */}
          <motion.div
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            onClick={onClose}
          />

          {/* Panel */}
          <motion.div
            className="glass relative z-10 w-full max-w-xl overflow-hidden rounded-[var(--radius-xl)] shadow-[var(--shadow-xl)]"
            initial={{ opacity: 0, scale: 0.95, y: -12 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -12 }}
            transition={{ duration: 0.18, ease: [0.34, 1.56, 0.64, 1] }}
            onKeyDown={handleKeyDown}
          >
            {/* Search input */}
            <div className="flex items-center gap-3 border-b border-[var(--glass-border)] px-4 py-3.5">
              <Search className="h-4 w-4 shrink-0 text-[var(--muted-foreground)]" />
              <input
                ref={inputRef}
                value={query}
                onChange={(e) => { setQuery(e.target.value); setHighlighted(0); }}
                placeholder="Buscar páginas, conversas, ações..."
                className="flex-1 bg-transparent text-[var(--text-sm)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)] focus:outline-none"
                aria-label="Buscar comandos"
                autoComplete="off"
              />
              <kbd className="rounded border border-[var(--border)] bg-[var(--surface-2)] px-1.5 py-0.5 text-[0.6rem] text-[var(--muted-foreground)]">
                ESC
              </kbd>
            </div>

            {/* Results */}
            <div className="max-h-96 overflow-y-auto py-2">
              {flatItems.length === 0 ? (
                <div className="px-4 py-8 text-center text-[var(--text-sm)] text-[var(--muted-foreground)]">
                  Nenhum resultado para &ldquo;{query}&rdquo;
                </div>
              ) : (
                Object.entries(groups).map(([group, groupItems]) => {
                  const flatOffset = flatItems.findIndex((item) => item.id === groupItems[0].id);
                  return (
                    <div key={group}>
                      <div className="px-4 py-1.5 text-[0.6rem] font-semibold uppercase tracking-widest text-[var(--muted-foreground)]">
                        {group}
                      </div>
                      {groupItems.map((item, i) => {
                        const idx = flatOffset + i;
                        return (
                          <button
                            key={item.id}
                            type="button"
                            onMouseEnter={() => setHighlighted(idx)}
                            onClick={item.action}
                            className={cn(
                              'flex w-full items-center gap-3 px-4 py-2.5 text-left transition-colors',
                              idx === highlighted
                                ? 'bg-[var(--surface-3)] text-[var(--brand-primary)]'
                                : 'text-[var(--foreground)]'
                            )}
                          >
                            <span className={cn('shrink-0', idx === highlighted ? 'text-[var(--brand-primary)]' : 'text-[var(--muted-foreground)]')}>
                              {item.icon}
                            </span>
                            <span className="flex-1 min-w-0">
                              <span className="block text-[var(--text-sm)] font-medium truncate">{item.label}</span>
                              {item.description && (
                                <span className="block text-[0.65rem] text-[var(--muted-foreground)] truncate">{item.description}</span>
                              )}
                            </span>
                            {idx === highlighted && (
                              <ArrowRight className="h-3.5 w-3.5 shrink-0 text-[var(--brand-primary)]" />
                            )}
                          </button>
                        );
                      })}
                    </div>
                  );
                })
              )}
            </div>

            {/* Footer hint bar */}
            <div className="flex items-center gap-4 border-t border-[var(--glass-border)] px-4 py-2.5">
              {[
                { key: '↑↓', label: 'navegar' },
                { key: 'Enter', label: 'selecionar' },
                { key: 'Esc', label: 'fechar' },
              ].map(({ key, label }) => (
                <span key={key} className="flex items-center gap-1 text-[0.6rem] text-[var(--muted-foreground)]">
                  <kbd className="rounded border border-[var(--border)] bg-[var(--surface-2)] px-1.5 py-0.5 font-mono">{key}</kbd>
                  {label}
                </span>
              ))}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    document.body
  );
}

// Hook for global Cmd+K listener
export function useCommandPalette() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setOpen((v) => !v);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  return { open, setOpen };
}
