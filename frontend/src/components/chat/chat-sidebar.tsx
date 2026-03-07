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

/* SVG wordmark "Lume" — matches Claude.ai logo pattern: SVG text, h-4, fill currentColor */
function LumeLogo({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 68 16"
      className={className}
      height="16"
      fill="currentColor"
      aria-label="Lume"
    >
      <text
        x="0"
        y="13"
        style={{
          fontFamily: "var(--font-display), Georgia, 'Times New Roman', serif",
          fontSize: '15px',
          fontWeight: 600,
          letterSpacing: '-0.3px',
        }}
      >
        Lume
      </text>
    </svg>
  );
}

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
      {/* Header: Logo + toggle — Claude exact: logo as SVG wordmark, h-4, flex-shrink-0 */}
      <div className="flex items-center justify-between px-4 pt-3 pb-1">
        <a
          onClick={(e) => { e.preventDefault(); handleNewChat(); }}
          href="#"
          className="flex flex-col justify-start items-start"
          aria-label="Inicio"
        >
          <LumeLogo className="h-4 flex-shrink-0 text-[var(--foreground)]" />
        </a>
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

      {/* Primary nav — Claude exact: inline-flex, 12px, weight 400, h-8, py-1.5 px-4, radius 6px */}
      <nav className="flex flex-col px-2 mt-1 gap-0.5">
        <button
          onClick={handleNewChat}
          className="inline-flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] font-normal text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors"
        >
          <Plus className="h-4 w-4" />
          <span>Novo bate-papo</span>
        </button>

        <button
          onClick={() => setSearchOpen((v) => !v)}
          className="inline-flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] font-normal text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors"
        >
          <Search className="h-5 w-5" />
          <span>Procurar</span>
        </button>

        <button
          onClick={cycleTheme}
          className="inline-flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] font-normal text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors"
          title={`Tema: ${themeLabel}`}
        >
          <Sliders className="h-5 w-5" />
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
            className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] py-1.5 px-3 text-[13px] font-normal text-[var(--foreground)] placeholder:text-[var(--subtle-foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)] transition-colors"
          />
        </div>
      )}

      {/* Secondary nav — Claude exact: separate group, space above (no visible divider line) */}
      <nav className="flex flex-col px-2 mt-3 gap-0.5">
        <button className="inline-flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] font-normal text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors">
          <MessageSquare className="h-5 w-5" />
          <span>Conversas</span>
        </button>
        <button className="inline-flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] font-normal text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors">
          <FolderOpen className="h-5 w-5" />
          <span>Projetos</span>
        </button>
        <button className="inline-flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] font-normal text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors">
          <LayoutGrid className="h-5 w-5" />
          <span>Artefatos</span>
        </button>
        <button className="inline-flex items-center gap-2 h-8 px-4 py-1.5 rounded-[6px] text-[12px] font-normal text-[var(--foreground-secondary)] hover:bg-[var(--surface-active)] transition-colors">
          <Code2 className="h-5 w-5" />
          <span>Codigo</span>
        </button>
      </nav>

      {/* Recentes label + Conversation list — Claude exact: h2, 12px, weight 400, text-500 */}
      <div className="flex-1 overflow-y-auto mt-2">
        <h2 className="px-4 pb-2 mt-1 text-[12px] font-normal text-[var(--text-500)] select-none" style={{ lineHeight: '16px' }}>
          Recentes
        </h2>
        <div className="px-0">
          <ConversationList searchTerm={searchTerm} />
        </div>
      </div>

      {/* Footer: User profile — Claude exact: avatar + name + plan + download + chevron */}
      <div className="p-2">
        <div className="flex items-center gap-3 rounded-lg py-2 px-3 hover:bg-[var(--surface-active)] transition-colors cursor-pointer active:scale-[0.985]">
          <Avatar name={user?.fullName || 'U'} size="sm" />
          <div className="flex-1 min-w-0">
            <p className="truncate text-[13px] font-medium text-[var(--foreground)]">
              {user?.fullName || 'Usuario'}
            </p>
            <p className="truncate text-[11px] font-normal text-[var(--muted-foreground)]">
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
