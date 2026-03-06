import { cn } from '@/lib/cn';

type BadgeVariant = 'default' | 'brand' | 'success' | 'warning' | 'error' | 'info' | 'outline';

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
  dot?: boolean;
}

const variantStyles: Record<BadgeVariant, string> = {
  default: 'bg-[rgba(140,140,180,0.06)] text-[var(--foreground)] border-[var(--border)]',
  brand: 'bg-[rgba(124,106,255,0.1)] text-[#c4b5fd] border-[rgba(124,106,255,0.2)]',
  success: 'bg-[rgba(0,212,170,0.1)] text-[var(--success)] border-[rgba(0,212,170,0.2)]',
  warning: 'bg-[rgba(251,191,36,0.12)] text-[var(--warning)] border-[rgba(251,191,36,0.25)]',
  error: 'bg-[rgba(255,92,111,0.1)] text-[var(--destructive)] border-[rgba(255,92,111,0.2)]',
  info: 'bg-[rgba(96,165,250,0.1)] text-[var(--info)] border-[rgba(96,165,250,0.2)]',
  outline: 'bg-transparent text-[var(--foreground-muted)] border-[var(--border)]',
};

const dotStyles: Record<BadgeVariant, string> = {
  default: 'bg-[var(--foreground-muted)]',
  brand: 'bg-[var(--brand-primary)]',
  success: 'bg-[var(--success)]',
  warning: 'bg-[var(--warning)]',
  error: 'bg-[var(--destructive)]',
  info: 'bg-[var(--info)]',
  outline: 'bg-[var(--foreground-muted)]',
};

export function Badge({ variant = 'default', children, className, dot }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] border px-2.5 py-1',
        'text-[0.64rem] font-semibold leading-none uppercase tracking-[0.12em]',
        variantStyles[variant],
        className
      )}
    >
      {dot && <span className={cn('h-1.5 w-1.5 rounded-full shrink-0', dotStyles[variant])} />}
      {children}
    </span>
  );
}
