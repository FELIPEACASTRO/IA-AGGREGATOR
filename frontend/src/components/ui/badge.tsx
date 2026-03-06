import { cn } from '@/lib/cn';

type BadgeVariant = 'default' | 'brand' | 'success' | 'warning' | 'error' | 'info' | 'outline';

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
  dot?: boolean;
}

const variantStyles: Record<BadgeVariant, string> = {
  default: 'bg-[rgba(160,176,215,0.08)] text-[var(--foreground)] border-[var(--border)]',
  brand: 'bg-[rgba(96,115,255,0.12)] text-[#dfe6ff] border-[rgba(96,115,255,0.24)]',
  success: 'bg-[rgba(78,217,167,0.12)] text-[var(--success)] border-[rgba(78,217,167,0.24)]',
  warning: 'bg-[rgba(255,191,102,0.12)] text-[var(--warning)] border-[rgba(255,191,102,0.24)]',
  error: 'bg-[rgba(255,107,135,0.12)] text-[var(--destructive)] border-[rgba(255,107,135,0.24)]',
  info: 'bg-[rgba(119,184,255,0.12)] text-[var(--info)] border-[rgba(119,184,255,0.24)]',
  outline: 'bg-transparent text-[var(--muted-foreground)] border-[var(--border)]',
};

const dotStyles: Record<BadgeVariant, string> = {
  default: 'bg-[var(--muted-foreground)]',
  brand: 'bg-[var(--brand-primary)]',
  success: 'bg-[var(--success)]',
  warning: 'bg-[var(--warning)]',
  error: 'bg-[var(--destructive)]',
  info: 'bg-[var(--info)]',
  outline: 'bg-[var(--muted-foreground)]',
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
