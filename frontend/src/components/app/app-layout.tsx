'use client';

import { usePathname } from 'next/navigation';
import { ReactNode, useEffect, useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { useThemeStore } from '@/stores/theme-store';
import { Avatar } from '@/components/ui/avatar';
import { WorkspaceSwitcher } from '@/components/app/workspace-switcher';
import { SidebarNavItem } from '@/components/ui/sidebar-nav-item';
import {
  CommandPalette,
  consumerQuickActions,
  openCommandPalette,
} from '@/components/ui/command-palette';
import { LumeLogo } from '@/components/ui/lume-logo';
import {
  BookOpen,
  CreditCard,
  MessageSquare,
  Monitor,
  Moon,
  PanelLeft,
  Search,
  Settings,
  Sun,
  TerminalSquare,
  WandSparkles,
} from 'lucide-react';

type AppLayoutProps = {
  children: ReactNode;
};

const navItems = [
  { href: '/chat', label: 'Chat', icon: MessageSquare },
  { href: '/library', label: 'Biblioteca', icon: BookOpen },
  { href: '/prompts', label: 'Templates', icon: WandSparkles },
  { href: '/billing', label: 'Plano', icon: CreditCard },
  { href: '/settings', label: 'Configuracoes', icon: Settings },
];

export function AppLayout({ children }: AppLayoutProps) {
  const pathname = usePathname();
  const user = useAuthStore((state) => state.user);
  const loadConversations = useChatStore((state) => state.loadConversations);
  const theme = useThemeStore((state) => state.theme);
  const setTheme = useThemeStore((state) => state.setTheme);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (typeof fetch !== 'function') return;
    void loadConversations();
  }, [loadConversations]);

  const cycleTheme = () => {
    setTheme(theme === 'system' ? 'light' : theme === 'light' ? 'dark' : 'system');
  };

  const ThemeIcon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor;
  const themeLabel = theme === 'light' ? 'Claro' : theme === 'dark' ? 'Escuro' : 'Sistema';

  const sidebarContent = (
    <>
      <div className="border-b border-[var(--border)] px-4 py-4">
        <LumeLogo />
        <WorkspaceSwitcher compact className="mt-4" />
      </div>

      <div className="px-3 py-3">
        <button
          type="button"
          onClick={() => openCommandPalette('consumer')}
          className="flex h-11 w-full items-center gap-3 rounded-[9.6px] border border-[var(--border)] bg-[var(--surface)] px-3 text-[13px] text-[var(--muted-foreground)] transition-colors hover:border-[var(--border-strong)] hover:text-[var(--foreground)]"
        >
          <Search className="h-4 w-4" />
          <span className="flex-1 text-left">Buscar ou navegar</span>
          <span className="rounded-[var(--radius-sm)] border border-[var(--border)] px-1.5 py-0.5 text-[11px] text-[var(--subtle-foreground)]">
            Ctrl+K
          </span>
        </button>
      </div>

      <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 pb-3" aria-label="Navegacao principal">
        {navItems.map(({ href, label, icon }) => (
          <SidebarNavItem
            key={href}
            href={href}
            label={label}
            icon={icon}
            active={pathname === href || pathname.startsWith(`${href}/`)}
          />
        ))}
      </nav>

      <div className="space-y-2 border-t border-[var(--border)] p-3">
        <SidebarNavItem
          label={`Tema: ${themeLabel}`}
          icon={ThemeIcon}
          onClick={cycleTheme}
        />
        <SidebarNavItem
          href="/codex"
          label="Abrir Codex"
          icon={TerminalSquare}
        />
        <div className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-3 py-3">
          <Avatar name={user?.fullName || 'U'} size="md" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-[13px] font-medium text-[var(--foreground)]">{user?.fullName || 'Conta'}</p>
            <p className="truncate text-[11px] text-[var(--muted-foreground)]">{user?.email}</p>
          </div>
        </div>
      </div>
    </>
  );

  return (
    <div className="flex min-h-screen bg-[var(--background)] text-[var(--foreground)]">
      <CommandPalette scope="consumer" quickActions={consumerQuickActions} />

      <aside className="hidden w-[var(--sidebar-width)] shrink-0 flex-col border-r border-[var(--border)] bg-[var(--surface-sidebar)] md:flex">
        {sidebarContent}
      </aside>

      {mobileOpen ? (
        <>
          <div
            className="fixed inset-0 z-[var(--z-modal)] bg-black/40 md:hidden"
            onClick={() => setMobileOpen(false)}
          />
          <aside className="fixed inset-y-0 left-0 z-[calc(var(--z-modal)+1)] flex w-[280px] flex-col border-r border-[var(--border)] bg-[var(--surface-sidebar)] shadow-[var(--shadow-lg)] md:hidden">
            {sidebarContent}
          </aside>
        </>
      ) : null}

      <div className="flex min-w-0 flex-1 flex-col">
        <header
          className="sticky top-0 z-[var(--z-sticky)] flex items-center justify-between border-b border-[var(--border)] px-4 py-3 backdrop-blur-md md:hidden"
          style={{ backgroundColor: 'color-mix(in srgb, var(--background) 88%, transparent)' }}
        >
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMobileOpen(true)}
              className="inline-flex h-9 w-9 items-center justify-center rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] text-[var(--foreground)]"
              aria-label="Abrir menu"
            >
              <PanelLeft className="h-4 w-4" />
            </button>
            <LumeLogo compact textClassName="hidden" />
          </div>
          <button
            type="button"
            onClick={() => openCommandPalette('consumer')}
            className="inline-flex h-9 items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-3 text-[12px] text-[var(--muted-foreground)]"
          >
            <Search className="h-4 w-4" />
            Buscar
          </button>
        </header>

        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
