'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode, useEffect, useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useThemeStore } from '@/stores/theme-store';
import { cn } from '@/lib/cn';
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
  Sparkles,
} from 'lucide-react';

type AppShellProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
  headerActions?: ReactNode;
  noPadding?: boolean;
};

const navItems = [
  { href: '/chat', label: 'Chat', icon: MessageSquare },
  { href: '/library', label: 'Biblioteca', icon: BookOpen },
  { href: '/prompts', label: 'Templates', icon: Zap },
  { href: '/billing', label: 'Plano', icon: CreditCard },
  { href: '/settings', label: 'Configuracoes', icon: Settings },
];

const SIDEBAR_KEY = 'lume-sidebar-collapsed';

function useSidebarState() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem(SIDEBAR_KEY);
    if (saved === 'true') setCollapsed(true);
  }, []);

  const toggle = () => {
    setCollapsed((current) => {
      const next = !current;
      localStorage.setItem(SIDEBAR_KEY, String(next));
      return next;
    });
  };

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'b') {
        event.preventDefault();
        toggle();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  return { collapsed, toggle, mobileOpen, setMobileOpen };
}

function ThemeCycler({ collapsed = false }: { collapsed?: boolean }) {
  const { theme, setTheme } = useThemeStore();

  const next = () => {
    setTheme(theme === 'system' ? 'light' : theme === 'light' ? 'dark' : 'system');
  };

  const Icon = theme === 'light' ? Sun : theme === 'dark' ? Moon : Monitor;
  const label = theme === 'light' ? 'Claro' : theme === 'dark' ? 'Escuro' : 'Sistema';

  return (
    <button
      onClick={next}
      className={cn(
        'flex items-center gap-3 rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-2 text-[var(--muted-foreground)] hover:border-[var(--border-strong)] hover:text-[var(--foreground)]',
        collapsed && 'justify-center px-0 py-2.5'
      )}
      title={`Tema: ${label}`}
      aria-label={`Tema: ${label}`}
    >
      <Icon className="h-4 w-4 shrink-0" />
      {!collapsed && <span className="truncate text-[0.78rem] font-medium">{label}</span>}
    </button>
  );
}

function SidebarContent({ collapsed, pathname, onNavigate, onLogout }: {
  collapsed: boolean;
  pathname: string;
  onNavigate?: () => void;
  onLogout: () => void;
}) {
  const user = useAuthStore((state) => state.user);
  const { open: cmdOpen, setOpen: setCmdOpen } = useCommandPalette();

  return (
    <>
      <div className={cn('flex items-center border-b border-[var(--border)] px-4 py-4', collapsed ? 'justify-center' : 'justify-between')}>
        <Link href="/chat" onClick={onNavigate} className="inline-flex items-center gap-3 min-w-0">
          <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] text-white shadow-[var(--shadow-brand)]" style={{ background: 'var(--brand-gradient)' }}>
            <Bot className="h-5 w-5" />
          </span>
          {!collapsed && (
            <span className="min-w-0">
              <span className="block truncate text-[1rem] font-semibold tracking-[-0.05em] text-[var(--foreground)]">Lume</span>
              <span className="block truncate text-[0.68rem] uppercase tracking-[0.2em] text-[var(--subtle-foreground)]">AI Workspace</span>
            </span>
          )}
        </Link>
        {!collapsed && (
          <button
            onClick={() => setCmdOpen(true)}
            className="hidden items-center gap-2 rounded-full border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-2 text-[0.74rem] text-[var(--muted-foreground)] hover:border-[var(--border-strong)] hover:text-[var(--foreground)] lg:inline-flex"
            aria-label="Abrir busca"
          >
            <Search className="h-3.5 w-3.5" />
            Buscar
            <kbd className="rounded-full border border-[var(--border)] px-1.5 py-0.5 text-[0.62rem]">CMD+K</kbd>
          </button>
        )}
      </div>

      <div className={cn('px-3 pt-3', collapsed && 'px-2')}>
        {!collapsed ? (
          <button
            onClick={() => setCmdOpen(true)}
            className="flex w-full items-center gap-3 rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-3 text-[0.82rem] text-[var(--muted-foreground)] hover:border-[var(--border-strong)] hover:text-[var(--foreground)] lg:hidden"
            aria-label="Abrir busca"
          >
            <Search className="h-4 w-4" />
            Buscar, paginas e acoes
          </button>
        ) : (
          <button
            onClick={() => setCmdOpen(true)}
            className="flex h-11 w-full items-center justify-center rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] text-[var(--muted-foreground)] hover:border-[var(--border-strong)] hover:text-[var(--foreground)]"
            aria-label="Abrir busca"
          >
            <Search className="h-4 w-4" />
          </button>
        )}
      </div>

      <nav className="mt-4 flex-1 space-y-1 px-3 pb-4 overflow-y-auto" aria-label="Navegacao principal">
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
          return (
            <Link
              key={href}
              href={href}
              onClick={onNavigate}
              title={collapsed ? label : undefined}
              className={cn(
                'group flex items-center gap-3 rounded-[var(--radius-pill)] border px-3 py-3 transition-all',
                active
                  ? 'border-[rgba(96,115,255,0.28)] bg-[rgba(96,115,255,0.12)] text-[#e4e9ff] shadow-[inset_0_1px_0_rgba(255,255,255,0.02)]'
                  : 'border-transparent text-[var(--muted-foreground)] hover:border-[var(--border)] hover:bg-[rgba(255,255,255,0.03)] hover:text-[var(--foreground)]',
                collapsed && 'justify-center px-0'
              )}
              aria-current={active ? 'page' : undefined}
            >
              <Icon className={cn('h-4.5 w-4.5 shrink-0', active && 'text-[var(--brand-primary)]')} />
              {!collapsed && (
                <>
                  <span className="truncate text-[0.84rem] font-medium">{label}</span>
                  {active && <Sparkles className="ml-auto h-3.5 w-3.5 shrink-0 text-[var(--brand-primary)]" />}
                </>
              )}
            </Link>
          );
        })}
      </nav>

      <div className={cn('space-y-2 border-t border-[var(--border)] p-3', collapsed && 'items-center')}>
        <ThemeCycler collapsed={collapsed} />
        <div className={cn('rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-3', collapsed && 'flex flex-col items-center px-2')}>
          <div className={cn('flex items-center gap-3', collapsed && 'flex-col')}>
            <Avatar name={user?.fullName || 'Lume'} size={collapsed ? 'sm' : 'md'} />
            {!collapsed && (
              <div className="min-w-0 flex-1">
                <p className="truncate text-[0.82rem] font-semibold text-[var(--foreground)]">{user?.fullName || 'Conta Lume'}</p>
                <p className="truncate text-[0.7rem] text-[var(--muted-foreground)]">{user?.email}</p>
              </div>
            )}
          </div>
          <button
            onClick={onLogout}
            className={cn(
              'mt-3 inline-flex items-center justify-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-3 py-2 text-[0.76rem] font-medium text-[var(--muted-foreground)] hover:border-[rgba(255,107,135,0.24)] hover:text-[var(--destructive)]',
              collapsed && 'w-full px-0'
            )}
            aria-label="Sair"
            title="Sair"
          >
            <LogOut className="h-4 w-4" />
            {!collapsed && 'Sair'}
          </button>
        </div>
      </div>

      <CommandPalette open={cmdOpen} onClose={() => setCmdOpen(false)} />
    </>
  );
}

export function AppShell({ title, subtitle, children, headerActions, noPadding }: AppShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const { collapsed, toggle, mobileOpen, setMobileOpen } = useSidebarState();

  const handleLogout = () => {
    useAuthStore.getState().logout();
    router.push('/login');
  };

  return (
    <div className="min-h-screen px-3 py-3 md:px-4 md:py-4">
      <div className="lume-page flex min-h-[calc(100vh-1.5rem)] gap-3 md:gap-4">
        {mobileOpen && (
          <button
            className="fixed inset-0 z-[var(--z-modal)] bg-[rgba(3,8,18,0.72)] backdrop-blur-md md:hidden"
            onClick={() => setMobileOpen(false)}
            aria-label="Fechar menu"
          />
        )}

        <aside
          className={cn(
            'lume-shell relative hidden min-h-[calc(100vh-1.5rem)] flex-col overflow-hidden rounded-[var(--radius-2xl)] md:flex',
            collapsed ? 'w-[92px]' : 'w-[312px]'
          )}
        >
          <SidebarContent collapsed={collapsed} pathname={pathname} onLogout={handleLogout} />
          <button
            onClick={toggle}
            className={cn(
              'absolute bottom-5 hidden h-10 w-10 items-center justify-center rounded-full border border-[var(--border)] bg-[rgba(8,17,31,0.94)] text-[var(--muted-foreground)] hover:border-[var(--border-strong)] hover:text-[var(--foreground)] md:inline-flex',
              collapsed ? 'left-1/2 -translate-x-1/2' : 'right-5'
            )}
            aria-label={collapsed ? 'Expandir menu' : 'Recolher menu'}
            title={collapsed ? 'Expandir menu' : 'Recolher menu'}
          >
            <ChevronLeft className={cn('h-4 w-4 transition-transform', collapsed && 'rotate-180')} />
          </button>
        </aside>

        <aside
          className={cn(
            'fixed inset-y-3 left-3 z-[calc(var(--z-modal)+1)] flex w-[min(22rem,calc(100vw-1.5rem))] flex-col overflow-hidden rounded-[var(--radius-2xl)] md:hidden',
            'lume-shell transition-transform duration-[var(--dur-slow)] ease-[var(--ease-standard)]',
            mobileOpen ? 'translate-x-0' : '-translate-x-[110%]'
          )}
        >
          <SidebarContent collapsed={false} pathname={pathname} onNavigate={() => setMobileOpen(false)} onLogout={handleLogout} />
        </aside>

        <div className="flex min-w-0 flex-1 flex-col gap-3 md:gap-4">
          <header className="lume-shell rounded-[var(--radius-2xl)] px-4 py-4 md:px-6 md:py-5">
            <div className="flex flex-wrap items-start gap-4 md:items-center md:justify-between">
              <div className="flex items-start gap-3 md:items-center">
                <button
                  onClick={() => setMobileOpen(true)}
                  className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-[var(--border)] bg-[rgba(255,255,255,0.03)] text-[var(--foreground)] md:hidden"
                  aria-label="Abrir menu"
                >
                  <PanelLeft className="h-5 w-5" />
                </button>
                <div>
                  <p className="text-[0.68rem] font-semibold uppercase tracking-[0.2em] text-[var(--subtle-foreground)]">Lume workspace</p>
                  <h1 className="mt-1 text-[var(--text-2xl)] font-semibold text-[var(--foreground)]">{title}</h1>
                  {subtitle && <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">{subtitle}</p>}
                </div>
              </div>
              <div className="flex w-full flex-wrap items-center gap-2 md:w-auto md:justify-end">
                <span className="hidden items-center gap-2 rounded-full border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-2 text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)] lg:inline-flex">
                  Live workspace
                </span>
                {headerActions}
              </div>
            </div>
          </header>

          <main
            id="main-content"
            className={cn(
              'lume-shell flex-1 overflow-hidden rounded-[var(--radius-2xl)]',
              noPadding ? 'p-0' : 'px-4 py-4 md:px-6 md:py-6'
            )}
          >
            {children}
          </main>
        </div>
      </div>

      <nav className="fixed bottom-3 left-3 right-3 z-[var(--z-sticky)] grid grid-cols-5 gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(8,17,31,0.92)] p-2 shadow-[var(--shadow-lg)] backdrop-blur-md md:hidden" aria-label="Atalhos rapidos">
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || pathname.startsWith(`${href}/`);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                'flex flex-col items-center gap-1 rounded-[var(--radius-pill)] px-2 py-2 text-center transition-colors',
                active ? 'bg-[rgba(96,115,255,0.14)] text-[#dfe6ff]' : 'text-[var(--muted-foreground)]'
              )}
              aria-current={active ? 'page' : undefined}
            >
              <Icon className="h-4.5 w-4.5" />
              <span className="text-[0.58rem] font-semibold">{label.split(' ')[0]}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
