'use client';

import { useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { useThemeStore } from '@/stores/theme-store';
import { Avatar } from '@/components/ui/avatar';
import { ConversationList } from './conversation-list';
import {
  Search,
  MessageSquare,
  FolderOpen,
  LayoutGrid,
  Code2,
  Sliders,
  Plus,
  ChevronDown,
  Download,
} from 'lucide-react';

export function ChatSidebar() {
  const { user } = useAuthStore();
  const { createConversation } = useChatStore();
  const { theme, setTheme } = useThemeStore();
  const [searchTerm, setSearchTerm] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);

  const handleNewChat = () => {
    createConversation();
  };

  const cycleTheme = () => {
    setTheme(theme === 'system' ? 'light' : theme === 'light' ? 'dark' : 'system');
  };

  const themeLabel = theme === 'light' ? 'Claro' : theme === 'dark' ? 'Escuro' : 'Sistema';

  return (
    <div className="flex h-full flex-col">
      {/* Header: Logo text - Claude exact: font-serif "Claude" */}
      <div className="flex items-center justify-between px-4 pt-3 pb-1">
        <a
          onClick={(e) => { e.preventDefault(); handleNewChat(); }}
          href="#"
          className="text-[18px] font-semibold tracking-[-0.02em] text-[var(--foreground)] hover:opacity-80 transition-opacity"
          aria-label="Inicio"
        >
          Lume
        </a>
        {/* Sidebar toggle icon (cosmetic, matches Claude top-right icon) */}
        <button
          className="flex h-7 w-7 items-center justify-center rounded-[var(--radius-sm)] text-[var(--muted-foreground)] hover:bg-[var(--surface-active)] hover:text-[var(--foreground)] transition-colors"
          aria-label="Alternar painel"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <path d="M9 3v18" />
          </svg>
        </button>
      </div>

      {/* Primary nav items - Claude exact: py-1.5 px-4 text-[12px] h-8 rounded-[6px] gap-2 */}
      <nav className="flex flex-col px-2 mt-1 gap-0.5">
        <button
          onClick={handleNewChat}
          className="flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors active:scale-[1.0]"
        >
          <Plus className="h-4 w-4" />
          <span>Novo bate-papo</span>
        </button>

        <button
          onClick={() => setSearchOpen((v) => !v)}
          className="flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors active:scale-[1.0]"
        >
          <Search className="h-4 w-4" />
          <span>Procurar</span>
        </button>

        <button
          onClick={cycleTheme}
          className="flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors active:scale-[1.0]"
          title={`Tema: ${themeLabel}`}
        >
          <Sliders className="h-4 w-4" />
          <span>Personalizar</span>
        </button>
      </nav>

      {/* Search (collapsible) */}
      {searchOpen && (
        <div className="px-3 pb-2 pt-1">
          <input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Buscar conversas..."
            autoFocus
            className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] py-1.5 px-3 text-[13px] text-[var(--foreground)] placeholder:text-[var(--subtle-foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)] transition-colors"
          />
        </div>
      )}

      {/* Separator */}
      <div className="mx-4 my-1.5 h-px bg-[var(--border)]" />

      {/* Secondary nav - Claude exact: separated section with different icons */}
      <nav className="flex flex-col px-2 gap-0.5">
        <button className="flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors active:scale-[1.0]">
          <MessageSquare className="h-4 w-4" />
          <span>Conversas</span>
        </button>
        <button className="flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors active:scale-[1.0]">
          <FolderOpen className="h-4 w-4" />
          <span>Projetos</span>
        </button>
        <button className="flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors active:scale-[1.0]">
          <LayoutGrid className="h-4 w-4" />
          <span>Artefatos</span>
        </button>
        <button className="flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors active:scale-[1.0]">
          <Code2 className="h-4 w-4" />
          <span>Codigo</span>
        </button>
      </nav>

      {/* Recentes label + Conversation list */}
      <div className="flex-1 overflow-y-auto mt-2">
        <p className="px-4 py-1.5 text-[11px] font-medium text-[var(--subtle-foreground)]">
          Recentes
        </p>
        <div className="px-0">
          <ConversationList searchTerm={searchTerm} />
        </div>
      </div>

      {/* Footer: User profile - Claude exact: avatar + name + plan + icons */}
      <div className="p-2">
        <div className="flex items-center gap-3 rounded-lg py-2 px-3 hover:bg-[var(--surface-active)] transition-colors cursor-pointer active:scale-[0.985]">
          <Avatar name={user?.fullName || 'U'} size="sm" />
          <div className="flex-1 min-w-0">
            <p className="truncate text-[13px] font-medium text-[var(--foreground)]">
              {user?.fullName || 'Usuario'}
            </p>
            <p className="truncate text-[11px] text-[var(--muted-foreground)]">
              Plano Pro
            </p>
          </div>
          <div className="flex items-center gap-1">
            <Download className="h-3.5 w-3.5 text-[var(--muted-foreground)]" />
            <ChevronDown className="h-3.5 w-3.5 text-[var(--muted-foreground)]" />
          </div>
        </div>
      </div>
    </div>
  );
}
