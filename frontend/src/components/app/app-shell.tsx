'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode, useState, useEffect } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useThemeStore } from '@/stores/theme-store';
import { cn } from '@/lib/cn';
import { Button } from '@/components/ui/button';
import { Avatar } from '@/components/ui/avatar';
import { CommandPalette, useCommandPalette } from '@/components/ui/command-palette';
import {
  Bot,
  MessageSquare,
  BookOpen,
  Zap,
  CreditCard,
  Settings,
  LogOut,
  Sun,
  Moon,
  Monitor,
  ChevronLeft,
  PanelLeft,
  Search,
} from 'lucide-react';

type AppShellProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
  headerActions?: ReactNode;
  noPadding?: boolean;
};

const navItems = [
  { href: '/chat',     label: 'Chat',          icon: MessageSquare },
  { href: '/library',  label: 'Biblioteca',    icon: BookOpen },
  { href: '/prompts',  label: 'Prompts',        icon: Zap },
  { href: '/billing',  label: 'Plano',          icon: CreditCard },
  { href: '/settings', label: 'Configurações',  icon: Settings },
];

// Persist sidebar state to localStorage
const SIDEBAR_KEY = 'ia-sidebar-collapsed';

function useSidebarState() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem(SIDEBAR_KEY);
    if (saved === 'true') setCollapsed(true);
  }, []);

  const toggle = () => {
    setCollapsed((v) => {
      localStorage.setItem(SIDEBAR_KEY, String(!v));
      return !v;
    });
  };

  // Ctrl+B shortcut
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'b') {
        e.preventDefault();
        toggle();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  return { collapsed, toggle, mobileOpen, setMobileOpen };
}

function ThemeCycler() {
  const { theme, setTheme } = useThemeStore();

  const next = () => {
    setTheme(theme === 'system' ? 'light' : theme === 'light' ? 'dark' : 'system');
  };

  const Icon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor;
  const label = theme === 'light' ? 'Modo claro' : theme === 'dark' ? 'Modo escuro' : 'Sistema';

  return (
    <button
      onClick={next}
      className="group flex w-full items-center gap-3 rounded-[var(--radius-md)] px-3 py-2.5 text-[var(--muted-foreground)] transition-colors hover:bg-[var(--surface-2)] hover:text-[var(--foreground)]"
      title={label}
      aria-label={`Tema: ${label}`}
    >
      <Icon className="h-4.5 w-4.5 shrink-0" />
      <span className="truncate text-[var(--text-sm)]">{label}</span>
    </button>
  );
}

export function AppShell({ title, subtitle, children, headerActions, noPadding }: AppShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const { collapsed, toggle, mobileOpen, setMobileOpen } = useSidebarState();
  const { open: cmdOpen, setOpen: setCmdOpen } = useCommandPalette();

  const handleLogout = () => {
    useAuthStore.getState().logout();
    router.push('/login');
  };

  return (
    <>
      <div className="flex min-h-screen bg-[var(--background)] text-[var(--foreground)]">

        {/* ── MOBILE OVERLAY ────────────────────────────────────────────────── */}
        {mobileOpen && (
          <div
            className="fixed inset-0 z-[var(--z-modal)] bg-black/60 backdrop-blur-sm md:hidden"
            onClick={() => setMobileOpen(false)}
          />
        )}

        {/* ── LEFT SIDEBAR ──────────────────────────────────────────────────── */}
        <aside
          className={cn(
            'fixed top-0 left-0 z-[var(--z-sticky)] flex h-full flex-col border-r border-[var(--border)]',
            'bg-[var(--surface-1)] transition-[width,transform] duration-[var(--dur-slow)] ease-[var(--ease-standard)]',
            // Desktop
            'hidden md:flex',
            collapsed ? 'w-16' : 'w-60',
          )}
          role="navigation"
          aria-label="Navegação principal"
        >
          {/* Logo + toggle */}
          <div className={cn('flex h-16 shrink-0 items-center border-b border-[var(--border)]', collapsed ? 'justify-center px-0' : 'justify-between px-4')}>
            {!collapsed && (
              <Link href="/chat" className="flex items-center gap-2.5 min-w-0">
                <span
                  className="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-[var(--radius-md)]"
                  style={{ background: 'var(--brand-gradient)' }}
                  aria-hidden
                >
                  <Bot className="h-4 w-4 text-white" />
                </span>
                <span className="truncate text-[var(--text-sm)] font-semibold gradient-text">
                  IA Aggregator
                </span>
              </Link>
            )}
            {collapsed && (
              <Link href="/chat" aria-label="IA Aggregator">
                <span
                  className="inline-flex h-7 w-7 items-center justify-center rounded-[var(--radius-md)]"
                  style={{ background: 'var(--brand-gradient)' }}
                >
                  <Bot className="h-4 w-4 text-white" />
                </span>
              </Link>
            )}
            {!collapsed && (
              <button
                onClick={toggle}
                className="rounded-[var(--radius-sm)] p-1.5 text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)] transition-colors"
                title="Recolher sidebar (Ctrl+B)"
                aria-label="Recolher menu"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
            )}
            {collapsed && (
              <button
                onClick={toggle}
                className="absolute -right-3 top-5 rounded-full border border-[var(--border)] bg-[var(--surface-1)] p-1 text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] shadow-[var(--shadow-md)] transition-colors"
                title="Expandir sidebar (Ctrl+B)"
                aria-label="Expandir menu"
              >
                <PanelLeft className="h-3 w-3" />
              </button>
            )}
          </div>

          {/* Search / Cmd+K trigger */}
          {!collapsed && (
            <div className="px-3 py-2.5">
              <button
                onClick={() => setCmdOpen(true)}
                className="flex w-full items-center gap-2.5 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-3 py-2 text-[var(--muted-foreground)] transition-colors hover:border-[var(--border-strong)] hover:text-[var(--foreground)]"
                aria-label="Abrir busca (Ctrl+K)"
              >
                <Search className="h-3.5 w-3.5 shrink-0" />
                <span className="flex-1 text-left text-[0.75rem]">Buscar...</span>
                <kbd className="rounded border border-[var(--border)] bg-[var(--surface-1)] px-1 py-0.5 text-[0.55rem] font-mono">
                  ⌘K
                </kbd>
              </button>
            </div>
          )}

          {/* Nav items */}
          <nav className="mt-1 flex-1 space-y-0.5 px-2 overflow-y-auto">
            {navItems.map(({ href, label, icon: Icon }) => {
              const active = pathname === href || pathname.startsWith(href + '/');
              return (
                <Link
                  key={href}
                  href={href}
                  title={collapsed ? label : undefined}
                  className={cn(
                    'flex items-center gap-3 rounded-[var(--radius-md)] px-3 py-2.5 transition-colors',
                    'text-[var(--text-sm)] font-medium',
                    active
                      ? 'bg-[var(--surface-3)] text-[var(--brand-primary)]'
                      : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)]',
                    collapsed && 'justify-center px-0'
                  )}
                  aria-current={active ? 'page' : undefined}
                >
                  <Icon
                    className={cn('h-4.5 w-4.5 shrink-0', active && 'text-[var(--brand-primary)]')}
                  />
                  {!collapsed && <span className="truncate">{label}</span>}
                  {active && !collapsed && (
                    <span className="ml-auto h-1.5 w-1.5 shrink-0 rounded-full bg-[var(--brand-primary)]" />
                  )}
                </Link>
              );
            })}
          </nav>

          {/* Bottom: theme + user + logout */}
          <div className={cn('border-t border-[var(--border)] p-2 space-y-0.5', collapsed && 'flex flex-col items-center')}>
            {collapsed ? (
              <>
                <ThemeCycler />
                {user && (
                  <div title={user.fullName || 'Usuário'} className="flex justify-center py-1">
                    <Avatar name={user.fullName || '?'} size="sm" />
                  </div>
                )}
                <button
                  onClick={handleLogout}
                  className="flex w-full justify-center rounded-[var(--radius-md)] p-2.5 text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--destructive)] transition-colors"
                  aria-label="Sair"
                  title="Sair"
                >
                  <LogOut className="h-4 w-4" />
                </button>
              </>
            ) : (
              <>
                <ThemeCycler />
                <div className="flex items-center gap-2.5 rounded-[var(--radius-md)] px-3 py-2.5">
                  <Avatar name={user?.fullName || '?'} size="sm" />
                  <div className="flex-1 min-w-0">
                    <p className="truncate text-[0.75rem] font-medium text-[var(--foreground)]">
                      {user?.fullName || 'Usuário'}
                    </p>
                    <p className="truncate text-[0.65rem] text-[var(--muted-foreground)]">
                      {user?.email}
                    </p>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="shrink-0 rounded-[var(--radius-sm)] p-1.5 text-[var(--muted-foreground)] hover:text-[var(--destructive)] hover:bg-[var(--surface-2)] transition-colors"
                    aria-label="Sair"
                    title="Sair"
                  >
                    <LogOut className="h-4 w-4" />
                  </button>
                </div>
              </>
            )}
          </div>
        </aside>

        {/* ── MOBILE SIDEBAR (off-canvas) ──────────────────────────────────── */}
        <aside
          className={cn(
            'fixed top-0 left-0 z-[calc(var(--z-modal)+1)] flex h-full w-72 flex-col border-r border-[var(--border)]',
            'bg-[var(--surface-1)] transition-transform duration-[var(--dur-slow)] ease-[var(--ease-standard)]',
            'md:hidden',
            mobileOpen ? 'translate-x-0' : '-translate-x-full'
          )}
          role="navigation"
          aria-label="Navegação mobile"
        >
          <div className="flex h-16 items-center justify-between border-b border-[var(--border)] px-4">
            <Link href="/chat" className="flex items-center gap-2.5" onClick={() => setMobileOpen(false)}>
              <span className="inline-flex h-7 w-7 items-center justify-center rounded-[var(--radius-md)]" style={{ background: 'var(--brand-gradient)' }}>
                <Bot className="h-4 w-4 text-white" />
              </span>
              <span className="text-[var(--text-sm)] font-semibold gradient-text">IA Aggregator</span>
            </Link>
            <button onClick={() => setMobileOpen(false)} className="p-1.5 text-[var(--muted-foreground)] hover:text-[var(--foreground)]" aria-label="Fechar menu">
              <ChevronLeft className="h-5 w-5" />
            </button>
          </div>
          <nav className="flex-1 space-y-0.5 px-2 py-2 overflow-y-auto">
            {navItems.map(({ href, label, icon: Icon }) => {
              const active = pathname === href || pathname.startsWith(href + '/');
              return (
                <Link
                  key={href}
                  href={href}
                  onClick={() => setMobileOpen(false)}
                  className={cn(
                    'flex items-center gap-3 rounded-[var(--radius-md)] px-3 py-2.5',
                    'text-[var(--text-sm)] font-medium transition-colors',
                    active
                      ? 'bg-[var(--surface-3)] text-[var(--brand-primary)]'
                      : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)]'
                  )}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  {label}
                </Link>
              );
            })}
          </nav>
          <div className="border-t border-[var(--border)] p-2">
            <ThemeCycler />
            <div className="mt-1 flex items-center gap-2.5 px-3 py-2">
              <Avatar name={user?.fullName || '?'} size="sm" />
              <span className="flex-1 truncate text-[0.75rem]">{user?.fullName}</span>
              <button onClick={handleLogout} aria-label="Sair" className="text-[var(--muted-foreground)] hover:text-[var(--destructive)]">
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          </div>
        </aside>

        {/* ── MAIN CONTENT ──────────────────────────────────────────────────── */}
        <div
          className={cn(
            'flex min-h-screen flex-1 flex-col transition-[margin] duration-[var(--dur-slow)] ease-[var(--ease-standard)]',
            // Desktop offset for sidebar
            'md:ml-16',
            !collapsed && 'md:ml-60'
          )}
        >
          {/* Mobile top bar */}
          <header className="flex h-14 shrink-0 items-center justify-between border-b border-[var(--border)] bg-[var(--background)] px-4 md:hidden">
            <button
              onClick={() => setMobileOpen(true)}
              className="p-2 text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
              aria-label="Abrir menu"
            >
              <PanelLeft className="h-5 w-5" />
            </button>
            <span className="text-[var(--text-sm)] font-semibold gradient-text">IA Aggregator</span>
            <button onClick={() => setCmdOpen(true)} aria-label="Buscar" className="p-2 text-[var(--muted-foreground)] hover:text-[var(--foreground)]">
              <Search className="h-5 w-5" />
            </button>
          </header>

          {/* Page header */}
          <div className="border-b border-[var(--border)] bg-[var(--background)] px-4 py-4 md:px-6">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <h1 className="text-[var(--text-xl)] font-semibold leading-tight">{title}</h1>
                {subtitle && (
                  <p className="mt-0.5 truncate text-[var(--text-sm)] text-[var(--muted-foreground)]">
                    {subtitle}
                  </p>
                )}
              </div>
              {headerActions && <div className="shrink-0">{headerActions}</div>}
            </div>
          </div>

          {/* Page content */}
          <main
            id="main-content"
            className={cn(
              noPadding
                ? 'flex-1 overflow-hidden pb-[calc(4rem+env(safe-area-inset-bottom))] md:pb-0'
                : 'flex-1 px-4 py-4 pb-[calc(5rem+env(safe-area-inset-bottom))] md:px-6 md:py-6'
            )}
          >
            {children}
          </main>
        </div>
      </div>

      {/* Command palette (global) */}
      <CommandPalette open={cmdOpen} onClose={() => setCmdOpen(false)} />

      {/* Mobile bottom nav */}
      <nav
        className="fixed bottom-0 left-0 right-0 z-[var(--z-sticky)] flex h-16 items-center justify-around border-t border-[var(--border)] bg-[var(--background)] px-2 md:hidden"
        aria-label="Navegação rápida"
      >
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(href + '/');
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                'flex flex-col items-center gap-0.5 rounded-[var(--radius-md)] px-3 py-1.5 transition-colors',
                active ? 'text-[var(--brand-primary)]' : 'text-[var(--muted-foreground)]'
              )}
              aria-current={active ? 'page' : undefined}
              aria-label={label}
            >
              <Icon className="h-5 w-5" />
              <span className="text-[0.55rem] font-medium">{label.split(' ')[0]}</span>
            </Link>
          );
        })}
      </nav>
    </>
  );
}

