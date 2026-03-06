import { ReactNode } from 'react';
import { cn } from '@/lib/cn';

type AlertProps = {
  children: ReactNode;
  variant?: 'error' | 'info' | 'success';
  className?: string;
};

const variants: Record<NonNullable<AlertProps['variant']>, string> = {
  error: 'border-[rgba(255,107,135,0.28)] bg-[rgba(255,107,135,0.08)] text-[var(--destructive)]',
  info: 'border-[var(--border)] bg-[rgba(119,184,255,0.08)] text-[var(--foreground)]',
  success: 'border-[rgba(78,217,167,0.28)] bg-[rgba(78,217,167,0.08)] text-[var(--success)]',
};

export function Alert({ children, variant = 'info', className }: AlertProps) {
  return (
    <div
      className={cn('rounded-[var(--radius-md)] border px-4 py-3 text-[var(--text-sm)] shadow-[var(--shadow-sm)]', variants[variant], className)}
      role="status"
      aria-live="polite"
    >
      {children}
    </div>
  );
}
