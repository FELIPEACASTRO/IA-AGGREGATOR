'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ReactNode, useEffect, useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useThemeStore } from '@/stores/theme-store';
import { cn } from '@/lib/cn';
import { Avatar } from '@/components/ui/avatar';
import {
  House,
  MessageSquare,
  BookOpen,
  Zap,
  CreditCard,
  Settings,
  Sun,
  Moon,
  Monitor,
  PanelLeft,
  Sparkles,
} from 'lucide-react';

type AppLayoutProps = {
  children: ReactNode;
};

const navItems = [
  { href: '/home', label: 'Home', icon: House },
  { href: '/chat', label: 'Chat', icon: MessageSquare },
  { href: '/library', label: 'Biblioteca', icon: BookOpen },
  { href: '/prompts', label: 'Templates', icon: Zap },
  { href: '/billing', label: 'Plano', icon: CreditCard },
  { href: '/settings', label: 'Configurações', icon: Settings },
];

export function AppLayout({ children }: AppLayoutProps) {
  const pathname = usePathname();
  const { user } = useAuthStore();
  const { theme, setTheme } = useThemeStore();
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  const cycleTheme = () => {
    setTheme(theme === 'system' ? 'light' : theme === 'light' ? 'dark' : 'system');
  };

  const ThemeIcon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor;
  const themeLabel = theme === 'light' ? 'Claro' : theme === 'dark' ? 'Escuro' : 'Sistema';

  const sidebarContent = (
    <>
      <div className="flex items-center gap-3 border-b border-[var(--border)] px-4 py-4">
        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--accent)] text-white">
          <Sparkles className="h-4 w-4" />
        </div>
        <span className="text-[15px] font-semibold text-[var(--foreground)]">Lume</span>
      </div>

      <nav className="flex-1 space-y-0.5 overflow-y-auto px-3 py-3">
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
          return (
            <Link
              key={href}
              href={href}
              onClick={() => setMobileOpen(false)}
              className={cn(
                'flex items-center gap-3 rounded-[var(--radius-md)] px-3 py-2 text-[14px] transition-colors',
                active
                  ? 'bg-[var(--surface-hover)] font-medium text-[var(--foreground)]'
                  : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]',
              )}
            >
              <Icon className="h-4 w-4 shrink-0" />
              {label}
            </Link>
          );
        })}
      </nav>

      <div className="space-y-2 border-t border-[var(--border)] p-3">
        <button
          onClick={cycleTheme}
          className="flex w-full items-center gap-3 rounded-[var(--radius-md)] px-3 py-2 text-[13px] text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]"
        >
          <ThemeIcon className="h-4 w-4" />
          {themeLabel}
        </button>
        <div className="flex items-center gap-3 rounded-[var(--radius-md)] px-3 py-2">
          <Avatar name={user?.fullName || 'U'} size="sm" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-[13px] font-medium text-[var(--foreground)]">{user?.fullName || 'Conta'}</p>
            <p className="truncate text-[11px] text-[var(--muted-foreground)]">{user?.email}</p>
          </div>
        </div>
      </div>
    </>
  );

  return (
    <div className="flex h-screen overflow-hidden bg-[var(--background)]">
      <aside className="hidden w-[var(--sidebar-width)] shrink-0 flex-col border-r border-[var(--border)] bg-[var(--surface-sidebar)] md:flex">
        {sidebarContent}
      </aside>

      {mobileOpen && (
        <>
          <div
            className="fixed inset-0 z-[var(--z-modal)] bg-black/40 md:hidden"
            onClick={() => setMobileOpen(false)}
          />
          <aside className="fixed inset-y-0 left-0 z-[calc(var(--z-modal)+1)] flex w-[280px] flex-col bg-[var(--surface-sidebar)] shadow-[var(--shadow-lg)] md:hidden">
            {sidebarContent}
          </aside>
        </>
      )}

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <div className="flex items-center border-b border-[var(--border)] px-4 py-3 md:hidden">
          <button
            onClick={() => setMobileOpen(true)}
            className="mr-3 rounded-[var(--radius-md)] p-1.5 text-[var(--foreground)] hover:bg-[var(--surface-hover)]"
          >
            <PanelLeft className="h-5 w-5" />
          </button>
          <span className="text-[15px] font-semibold text-[var(--foreground)]">Lume</span>
        </div>

        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
