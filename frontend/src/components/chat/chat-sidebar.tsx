'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Moon, Search, Settings, SquarePen, Sun, Monitor, BookOpen, CreditCard, MessageSquare, WandSparkles } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { useThemeStore } from '@/stores/theme-store';
import { Avatar } from '@/components/ui/avatar';
import { ConversationList } from '@/components/chat/conversation-list';
import { SidebarNavItem } from '@/components/ui/sidebar-nav-item';
import { WorkspaceSwitcher } from '@/components/app/workspace-switcher';
import { LumeLogo } from '@/components/ui/lume-logo';
import { openCommandPalette } from '@/components/ui/command-palette';

const navItems = [
  { href: '/chat', label: 'Chat', icon: MessageSquare },
  { href: '/library', label: 'Biblioteca', icon: BookOpen },
  { href: '/prompts', label: 'Templates', icon: WandSparkles },
  { href: '/billing', label: 'Plano', icon: CreditCard },
  { href: '/settings', label: 'Configuracoes', icon: Settings },
];

export function ChatSidebar() {
  const pathname = usePathname();
  const user = useAuthStore((state) => state.user);
  const createConversation = useChatStore((state) => state.createConversation);
  const theme = useThemeStore((state) => state.theme);
  const setTheme = useThemeStore((state) => state.setTheme);

  const cycleTheme = () => {
    setTheme(theme === 'system' ? 'light' : theme === 'light' ? 'dark' : 'system');
  };

  const ThemeIcon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor;
  const themeLabel = theme === 'light' ? 'Claro' : theme === 'dark' ? 'Escuro' : 'Sistema';

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-[var(--border)] px-4 py-4">
        <div className="flex items-center justify-between gap-3">
          <Link href="/chat" className="inline-flex items-center">
            <LumeLogo />
          </Link>
          <button
            type="button"
            onClick={() => void createConversation()}
            className="inline-flex h-9 w-9 items-center justify-center rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] text-[var(--foreground)]"
            aria-label="Nova conversa"
          >
            <SquarePen className="h-4 w-4" />
          </button>
        </div>

        <WorkspaceSwitcher compact className="mt-4" />
      </div>

      <div className="px-3 py-3">
        <button
          type="button"
          onClick={() => openCommandPalette('consumer')}
          className="flex h-11 w-full items-center gap-3 rounded-[9.6px] border border-[var(--border)] bg-[var(--surface)] px-3 text-[13px] text-[var(--muted-foreground)] transition-colors hover:border-[var(--border-strong)] hover:text-[var(--foreground)]"
        >
          <Search className="h-4 w-4" />
          <span className="flex-1 text-left">Buscar conversas e paginas</span>
          <span className="rounded-[var(--radius-sm)] border border-[var(--border)] px-1.5 py-0.5 text-[11px] text-[var(--subtle-foreground)]">
            Ctrl+K
          </span>
        </button>
      </div>

      <nav className="space-y-0.5 px-3 pb-3">
        {navItems.map((item) => (
          <SidebarNavItem
            key={item.href}
            href={item.href}
            label={item.label}
            icon={item.icon}
            active={pathname === item.href || pathname.startsWith(`${item.href}/`)}
          />
        ))}
      </nav>

      <div className="flex-1 overflow-y-auto border-t border-[var(--border)]">
        <div className="px-4 pb-2 pt-4">
          <p className="text-[11px] uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Recentes</p>
        </div>
        <ConversationList searchTerm="" />
      </div>

      <div className="space-y-2 border-t border-[var(--border)] p-3">
        <SidebarNavItem label={`Tema: ${themeLabel}`} icon={ThemeIcon} onClick={cycleTheme} />
        <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-3 py-3">
          <Avatar name={user?.fullName || 'U'} size="md" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-[13px] font-medium text-[var(--foreground)]">{user?.fullName || 'Usuario'}</p>
            <p className="truncate text-[11px] text-[var(--muted-foreground)]">{user?.email}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
