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
import { useAuthStore } from '@/stores/auth-store';
import { WorkspaceSwitcher } from '@/components/app/workspace-switcher';
import { SidebarNavItem } from '@/components/ui/sidebar-nav-item';
import {
  codexQuickActions,
  CommandPalette,
  openCommandPalette,
} from '@/components/ui/command-palette';
import { LumeLogo } from '@/components/ui/lume-logo';

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
  { href: '/codex/settings/providers', label: 'Providers', icon: Bot },
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
  const logout = useAuthStore((state) => state.logout);

  const sections = useMemo(() => {
    return navItems.map((item) => {
      const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
      return { ...item, active };
    });
  }, [pathname]);

  return (
    <div className="min-h-screen bg-[var(--background)] text-[var(--foreground)]">
      <CommandPalette scope="codex" quickActions={codexQuickActions} />

      <div className="mx-auto grid max-w-[1700px] grid-cols-1 gap-4 px-3 py-3 lg:grid-cols-[296px_1fr] lg:px-4">
        <aside className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface)] p-3 shadow-[var(--shadow-md)]">
          <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-hover)] px-3 py-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <LumeLogo />
                <p className="mt-2 text-[11px] uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">
                  Codex Cloud
                </p>
              </div>
              <button
                type="button"
                onClick={() => openCommandPalette('codex')}
                className="inline-flex h-9 items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-3 text-[12px] text-[var(--muted-foreground)]"
              >
                <Activity className="h-4 w-4" />
                Ctrl+K
              </button>
            </div>

            <WorkspaceSwitcher compact className="mt-4" />
          </div>

          <nav className="mt-4 space-y-0.5" aria-label="Codex navigation">
            {sections.map((item) => (
              <SidebarNavItem
                key={item.href}
                href={item.href}
                label={item.label}
                icon={item.icon}
                active={item.active}
              />
            ))}
          </nav>

          <div className="mt-5 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-hover)] p-3">
            <p className="truncate text-[13px] font-medium text-[var(--foreground)]">{user?.fullName || 'Codex User'}</p>
            <p className="truncate text-[11px] text-[var(--muted-foreground)]">{user?.email || 'Sem sessao'}</p>
            <button
              className="mt-3 inline-flex h-9 w-full items-center justify-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-3 text-[12px] text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
              onClick={() => {
                void logout();
                router.push('/login');
              }}
            >
              <LogOut className="h-4 w-4" />
              Logout
            </button>
          </div>
        </aside>

        <main className="space-y-4">
          <header className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface)] px-5 py-5 shadow-[var(--shadow-md)]">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-[11px] uppercase tracking-[0.16em] text-[var(--subtle-foreground)]">Workspace task operations</p>
                <h1 className="mt-2 font-[var(--font-serif)] text-[28px] font-medium tracking-[-0.04em]">{title}</h1>
                {subtitle ? <p className="mt-2 max-w-3xl text-[14px] text-[var(--muted-foreground)]">{subtitle}</p> : null}
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <Link
                  href="/codex/tasks?tab=archived"
                  className="inline-flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-hover)] px-3 py-2 text-[12px] text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                >
                  <FolderGit2 className="h-4 w-4" />
                  Archived
                </Link>
                <Link
                  href="/codex/shortcuts"
                  className="inline-flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-hover)] px-3 py-2 text-[12px] text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                >
                  <ListChecks className="h-4 w-4" />
                  Shortcuts
                </Link>
                <Link
                  href="/codex/settings/usage/credits"
                  className="inline-flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-hover)] px-3 py-2 text-[12px] text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
                >
                  <WalletCards className="h-4 w-4" />
                  Credits
                </Link>
                <Link
                  href="/codex/settings/connectors"
                  className="inline-flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-hover)] px-3 py-2 text-[12px] text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
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
