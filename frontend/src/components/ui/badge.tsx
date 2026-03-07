'use client';

import { cn } from '@/lib/cn';

type BadgeVariant = 'default' | 'outline' | 'accent' | 'success' | 'warning' | 'error' | 'brand';

const variantStyles: Record<BadgeVariant, string> = {
  default: 'bg-[var(--surface-hover)] text-[var(--foreground)]',
  outline: 'border border-[var(--border)] text-[var(--muted-foreground)]',
  accent: 'bg-[var(--accent-light)] text-[var(--accent)]',
  brand: 'bg-[var(--accent-light)] text-[var(--accent)]',
  success: 'bg-[var(--success-light)] text-[var(--success)]',
  warning: 'bg-[var(--warning-light)] text-[var(--warning)]',
  error: 'bg-[var(--destructive-light)] text-[var(--destructive)]',
};

type BadgeProps = {
  variant?: BadgeVariant;
  dot?: boolean;
  children: React.ReactNode;
  className?: string;
};

export function Badge({ variant = 'default', dot, children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-[var(--radius-full)] px-2.5 py-0.5 text-[11px] font-medium',
        variantStyles[variant],
        className,
      )}
    >
      {dot && (
        <span
          className={cn(
            'h-1.5 w-1.5 rounded-full',
            variant === 'success' ? 'bg-[var(--success)]' :
            variant === 'error' ? 'bg-[var(--destructive)]' :
            variant === 'warning' ? 'bg-[var(--warning)]' :
            'bg-current',
          )}
        />
      )}
      {children}
    </span>
  );
}
