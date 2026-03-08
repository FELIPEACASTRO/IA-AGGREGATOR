'use client';

import { useTransition } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { cn } from '@/lib/cn';

type WorkspaceSwitcherProps = {
  compact?: boolean;
  className?: string;
};

export function WorkspaceSwitcher({ compact = false, className }: WorkspaceSwitcherProps) {
  const currentWorkspace = useAuthStore((state) => state.currentWorkspace);
  const workspaces = useAuthStore((state) => state.workspaces ?? []);
  const switchWorkspace = useAuthStore((state) => state.switchWorkspace);
  const [isPending, startTransition] = useTransition();

  const disabled = workspaces.length <= 1 || !currentWorkspace;

  return (
    <div className={cn('space-y-1', className)}>
      <p className="text-[11px] uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Workspace</p>
      <div className="rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-3 py-2">
        <p className={cn('truncate font-medium text-[var(--foreground)]', compact ? 'text-[12px]' : 'text-[13px]')}>
          {currentWorkspace?.name || 'Sem workspace'}
        </p>
        <div className="mt-1 flex items-center gap-2">
          <select
            aria-label="Selecionar workspace"
            value={currentWorkspace?.id || ''}
            disabled={disabled || isPending}
            onChange={(event) => {
              const nextWorkspaceId = event.target.value;
              startTransition(() => {
                void switchWorkspace(nextWorkspaceId);
              });
            }}
            className="h-8 w-full rounded-[var(--radius-sm)] border border-[var(--input-border)] bg-[var(--input-bg)] px-2 text-[12px] text-[var(--foreground)] outline-none focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--ring)] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {workspaces.length === 0 && <option value="">Sem workspaces</option>}
            {workspaces.map((membership) => (
              <option key={membership.workspace.id} value={membership.workspace.id}>
                {membership.workspace.name}
              </option>
            ))}
          </select>
          <span className="shrink-0 text-[11px] text-[var(--muted-foreground)]">
            {isPending ? 'Trocando...' : `${workspaces.length}`}
          </span>
        </div>
      </div>
    </div>
  );
}
