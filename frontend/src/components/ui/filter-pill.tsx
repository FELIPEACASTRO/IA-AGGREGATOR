'use client';

import { cn } from '@/lib/cn';

type FilterPillProps = {
  active?: boolean;
  onClick?: () => void;
  children: React.ReactNode;
  className?: string;
  count?: number;
};

export function FilterPill({
  active,
  onClick,
  children,
  className,
  count,
}: FilterPillProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'inline-flex items-center gap-1.5 rounded-[var(--radius-full)] border px-4 py-1.5 text-[13px] transition-colors',
        active
          ? 'border-[var(--foreground)] bg-[var(--foreground)] text-[var(--background)]'
          : 'border-[var(--border)] bg-transparent text-[var(--muted-foreground)] hover:border-[var(--border-strong)] hover:text-[var(--foreground)]',
        className,
      )}
    >
      {children}
      {typeof count === 'number' ? <span className="text-[11px] opacity-70">{count}</span> : null}
    </button>
  );
}
