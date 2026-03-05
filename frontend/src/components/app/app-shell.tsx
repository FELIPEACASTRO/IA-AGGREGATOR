'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { ReactNode } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { cn } from '@/lib/cn';
import { Button } from '@/components/ui/button';

type AppShellProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
  headerActions?: ReactNode;
};

const navItems = [
  { href: '/chat', label: 'Chat' },
  { href: '/library', label: 'Biblioteca' },
  { href: '/prompts', label: 'Prompts' },
  { href: '/billing', label: 'Plano' },
  { href: '/settings', label: 'Configurações' },
];

export function AppShell({ title, subtitle, children, headerActions }: AppShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const user = useAuthStore((s) => s.user);

  return (
    <main className="flex min-h-screen bg-[var(--background)] text-[var(--foreground)]">
      <aside className="hidden w-64 shrink-0 border-r border-[var(--border)] md:flex md:flex-col">
        <div className="px-5 py-4 border-b border-[var(--border)]">
          <p className="text-sm text-[var(--muted-foreground)]">IA Platform</p>
          <h1 className="text-lg font-bold">IA Aggregator</h1>
        </div>
        <nav className="p-3 space-y-1 flex-1">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'block rounded-lg px-3 py-2 text-sm transition',
                pathname === item.href
                  ? 'bg-[var(--secondary)] text-[var(--foreground)] font-medium'
                  : 'text-[var(--muted-foreground)] hover:bg-[var(--secondary)] hover:text-[var(--foreground)]'
              )}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="border-t border-[var(--border)] p-3 space-y-2">
          <p className="text-sm font-medium truncate">{user?.fullName || 'Usuário'}</p>
          <p className="text-xs text-[var(--muted-foreground)] truncate">{user?.email}</p>
          <Button
            variant="ghost"
            className="w-full justify-start px-2"
            onClick={() => {
              useAuthStore.getState().logout();
              router.push('/login');
            }}
          >
            Sair
          </Button>
        </div>
      </aside>

      <section className="flex min-w-0 flex-1 flex-col">
        <header className="border-b border-[var(--border)] px-4 py-3 md:px-6">
          <div className="flex items-center justify-between gap-4">
            <div className="min-w-0">
              <h2 className="text-lg font-semibold">{title}</h2>
              {subtitle ? (
                <p className="text-sm text-[var(--muted-foreground)] truncate">{subtitle}</p>
              ) : null}
            </div>
            {headerActions}
          </div>
          <nav className="mt-3 flex gap-2 overflow-x-auto pb-1 md:hidden">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'whitespace-nowrap rounded-lg border px-3 py-1.5 text-xs transition',
                  pathname === item.href
                    ? 'border-[var(--primary)] bg-[var(--secondary)] text-[var(--foreground)]'
                    : 'border-[var(--border)] text-[var(--muted-foreground)]'
                )}
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </header>
        <div className="flex-1 min-h-0">{children}</div>
      </section>
    </main>
  );
}
