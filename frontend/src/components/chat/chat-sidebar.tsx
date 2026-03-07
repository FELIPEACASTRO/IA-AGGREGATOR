'use client';

import { useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { useThemeStore } from '@/stores/theme-store';
import { Avatar } from '@/components/ui/avatar';
import { ConversationList } from './conversation-list';
import {
  Search,
  Sun,
  Moon,
  Monitor,
} from 'lucide-react';

/* Claude Web sidebar SVG logo wordmark */
function ClaudeLogo({ className }: { className?: string }) {
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
        fontFamily="-apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif"
        fontSize="15"
        fontWeight="600"
        letterSpacing="-0.5"
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

  const ThemeIcon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor;
  const themeLabel = theme === 'light' ? 'Claro' : theme === 'dark' ? 'Escuro' : 'Sistema';

  return (
    <div className="flex h-full flex-col">
      {/* Header: Logo + New chat icon - Claude exact: p-2, h-8 items, gap-1.5 */}
      <div className="relative flex w-full items-center p-2 pt-2">
        <div className="flex items-center gap-1.5 pl-2 h-8 overflow-hidden">
          {/* Logo link */}
          <a
            onClick={(e) => { e.preventDefault(); handleNewChat(); }}
            href="#"
            className="flex flex-col justify-start items-start"
            aria-label="Inicio"
          >
            <ClaudeLogo className="h-4 flex-shrink-0 text-[var(--foreground)]" />
          </a>
        </div>

        {/* Right side: search + new chat buttons */}
        <div className="ml-auto flex items-center gap-0.5">
          <button
            onClick={() => setSearchOpen((v) => !v)}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-[var(--muted-foreground)] hover:bg-[var(--surface-active)] hover:text-[var(--foreground)] transition-colors active:scale-95"
            aria-label="Buscar"
          >
            <Search className="h-4 w-4" />
          </button>
          {/* New chat button - Claude exact: h-8 w-8 rounded-lg */}
          <button
            onClick={handleNewChat}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-[var(--foreground)] hover:bg-[var(--surface-active)] transition-colors active:scale-95"
            aria-label="Novo bate-papo"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 20h9" />
              <path d="M16.376 3.622a1 1 0 0 1 3.002 3.002L7.368 18.635a2 2 0 0 1-.855.506l-2.872.838a.5.5 0 0 1-.62-.62l.838-2.872a2 2 0 0 1 .506-.854z" />
            </svg>
          </button>
        </div>
      </div>

      {/* Search (collapsible) */}
      {searchOpen && (
        <div className="px-2 pb-2">
          <input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Buscar conversas..."
            autoFocus
            className="w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] py-1.5 px-3 text-[13px] text-[var(--foreground)] placeholder:text-[var(--subtle-foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)] transition-colors"
          />
        </div>
      )}

      {/* Conversation list - Claude exact nav items: py-1.5 rounded-lg px-4 */}
      <div className="flex-1 overflow-y-auto px-2">
        <ConversationList searchTerm={searchTerm} />
      </div>

      {/* Footer - Claude style: no visible border, subtle items */}
      <div className="p-2 space-y-0.5">
        {/* Theme toggle */}
        <button
          onClick={cycleTheme}
          className="flex w-full items-center gap-3 rounded-lg py-1.5 px-3 text-[13px] text-[var(--muted-foreground)] hover:bg-[var(--surface-active)] hover:text-[var(--foreground)] transition-colors active:scale-[0.985]"
          title={`Tema: ${themeLabel}`}
        >
          <ThemeIcon className="h-4 w-4" />
          {themeLabel}
        </button>

        {/* User profile */}
        <div className="flex items-center gap-3 rounded-lg py-1.5 px-3 hover:bg-[var(--surface-active)] transition-colors cursor-pointer active:scale-[0.985]">
          <Avatar name={user?.fullName || 'U'} size="sm" />
          <div className="flex-1 min-w-0">
            <p className="truncate text-[13px] font-medium text-[var(--foreground)]">
              {user?.fullName || 'Usuario'}
            </p>
            <p className="truncate text-[11px] text-[var(--muted-foreground)]">
              Plano Pro
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
