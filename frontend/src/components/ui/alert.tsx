import { ReactNode } from 'react';
import { cn } from '@/lib/cn';

type AlertProps = {
  children: ReactNode;
  variant?: 'error' | 'info' | 'success';
  className?: string;
};

const variants: Record<NonNullable<AlertProps['variant']>, string> = {
  error: 'text-[var(--destructive)] bg-red-50 dark:bg-red-950/20 border-red-200 dark:border-red-900/50',
  info: 'text-[var(--foreground)] bg-[var(--secondary)] border-[var(--border)]',
  success: 'text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/50',
};

export function Alert({ children, variant = 'info', className }: AlertProps) {
  return (
    <div className={cn('rounded-lg border p-3 text-sm', variants[variant], className)} role="status" aria-live="polite">
      {children}
    </div>
  );
}
