import { cn } from '@/lib/cn';

type BadgeVariant = 'default' | 'brand' | 'success' | 'warning' | 'error' | 'info' | 'outline';

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
  dot?: boolean;
}

const variantStyles: Record<BadgeVariant, string> = {
  default: 'bg-[var(--surface-2)] text-[var(--foreground)] border-[var(--border)]',
  brand: 'bg-[var(--brand-primary)]/10 text-[var(--brand-primary)] border-[var(--brand-primary)]/20',
  success: 'bg-[var(--success)]/10 text-[var(--success)] border-[var(--success)]/20',
  warning: 'bg-[var(--warning)]/10 text-[var(--warning)] border-[var(--warning)]/20',
  error: 'bg-[var(--destructive)]/10 text-[var(--destructive)] border-[var(--destructive)]/20',
  info: 'bg-[var(--info)]/10 text-[var(--info)] border-[var(--info)]/20',
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
        'inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] border px-2 py-0.5',
        'text-[0.65rem] font-medium leading-none tracking-wide uppercase',
        variantStyles[variant],
        className
      )}
    >
      {dot && (
        <span className={cn('h-1.5 w-1.5 rounded-full shrink-0', dotStyles[variant])} />
      )}
      {children}
    </span>
  );
}
