'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode, useMemo } from 'react';
import {
  Activity,
  Bot,
  ChartSpline,
  ClipboardCheck,
  FolderGit2,
  Gauge,
  KeyRound,
  LayoutDashboard,
  Link2,
  ListChecks,
  LogOut,
  Settings,
  SlidersHorizontal,
  TerminalSquare,
  WalletCards,
  Wrench,
} from 'lucide-react';
import { cn } from '@/lib/cn';
import { useAuthStore } from '@/stores/auth-store';

type CodexShellProps = {
  title: string;
  subtitle?: string;
  headerActions?: ReactNode;
  children: ReactNode;
};

type NavItem = {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
};

const navItems: NavItem[] = [
  { href: '/codex', label: 'Tasks', icon: LayoutDashboard },
  { href: '/codex/settings/environments', label: 'Environments', icon: Wrench },
  { href: '/codex/settings/connectors', label: 'Connectors', icon: Link2 },
  { href: '/codex/settings/code-review', label: 'Code Review', icon: ClipboardCheck },
  { href: '/codex/settings/usage', label: 'Usage', icon: Gauge },
  { href: '/codex/settings/analytics', label: 'Analytics', icon: ChartSpline },
  { href: '/codex/settings/managed-configs', label: 'Managed Configs', icon: SlidersHorizontal },
  { href: '/codex/settings/apireference', label: 'API', icon: TerminalSquare },
  { href: '/codex/shortcuts', label: 'Shortcuts', icon: KeyRound },
  { href: '/admin/settings', label: 'Admin', icon: Settings },
];

export function CodexShell({ title, subtitle, headerActions, children }: CodexShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const user = useAuthStore((state) => state.user);

  const sections = useMemo(() => {
    return navItems.map((item) => {
      const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
      return { ...item, active };
    });
  }, [pathname]);

  return (
    <div className="min-h-screen bg-[var(--background)] text-[var(--foreground)]">
      <div className="mx-auto grid max-w-[1700px] grid-cols-1 gap-4 px-3 py-3 lg:grid-cols-[280px_1fr] lg:px-4">
        <aside className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(5,12,24,0.85)] p-3 shadow-[var(--shadow-lg)]">
          <div className="mb-4 flex items-center gap-3 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-3">
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-[var(--brand-gradient)] text-white">
              <Bot className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold tracking-[-0.03em]">Lume Codex Cloud</p>
              <p className="truncate text-xs text-[var(--muted-foreground)]">Task execution platform</p>
            </div>
          </div>

          <nav className="space-y-1" aria-label="Codex navigation">
            {sections.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-[var(--radius-pill)] border px-3 py-2.5 text-sm',
                  item.active
                    ? 'border-[rgba(96,115,255,0.4)] bg-[rgba(96,115,255,0.18)] text-[var(--foreground)]'
                    : 'border-transparent text-[var(--muted-foreground)] hover:border-[var(--border)] hover:bg-[rgba(255,255,255,0.04)] hover:text-[var(--foreground)]'
                )}
              >
                <item.icon className="h-4 w-4" />
                <span className="truncate">{item.label}</span>
              </Link>
            ))}
          </nav>

          <div className="mt-5 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-3">
            <p className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Workspace</p>
            <p className="mt-1 truncate text-sm font-medium">{user?.fullName || 'Codex User'}</p>
            <p className="truncate text-xs text-[var(--muted-foreground)]">{user?.email || 'Sem sessao'}</p>
            <button
              className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
              onClick={() => {
                useAuthStore.getState().logout();
                router.push('/login');
              }}
            >
              <LogOut className="h-4 w-4" />
              Logout
            </button>
          </div>
        </aside>

        <main className="space-y-4">
          <header className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(5,12,24,0.82)] px-5 py-4 shadow-[var(--shadow-lg)]">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Cloud Coding Operations</p>
                <h1 className="mt-1 text-2xl font-semibold tracking-[-0.04em]">{title}</h1>
                {subtitle && <p className="mt-1 text-sm text-[var(--muted-foreground)]">{subtitle}</p>}
              </div>
              <div className="flex items-center gap-2">
                <Link
                  href="/codex/tasks?tab=archived"
                  className="inline-flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                >
                  <FolderGit2 className="h-4 w-4" />
                  Archived
                </Link>
                <Link
                  href="/codex/shortcuts"
                  className="inline-flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                >
                  <ListChecks className="h-4 w-4" />
                  Shortcuts
                </Link>
                <Link
                  href="/codex/settings/usage/credits"
                  className="inline-flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                >
                  <WalletCards className="h-4 w-4" />
                  Credits
                </Link>
                <Link
                  href="/codex/settings/connectors"
                  className="inline-flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                >
                  <Activity className="h-4 w-4" />
                  Connectors
                </Link>
                {headerActions}
              </div>
            </div>
          </header>
          {children}
        </main>
      </div>
    </div>
  );
}

