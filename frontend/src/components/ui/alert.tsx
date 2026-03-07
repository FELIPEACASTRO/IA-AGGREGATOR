'use client';

import { cn } from '@/lib/cn';
import { AlertCircle, CheckCircle, Info } from 'lucide-react';

type AlertVariant = 'error' | 'success' | 'info';

const variantStyles: Record<AlertVariant, { container: string; icon: React.ElementType }> = {
  error: {
    container: 'border-[var(--destructive)] bg-[var(--destructive-light)] text-[var(--destructive)]',
    icon: AlertCircle,
  },
  success: {
    container: 'border-[var(--success)] bg-[var(--success-light)] text-[var(--success)]',
    icon: CheckCircle,
  },
  info: {
    container: 'border-[var(--info)] bg-[var(--surface)] text-[var(--info)]',
    icon: Info,
  },
};

type AlertProps = {
  variant?: AlertVariant;
  children: React.ReactNode;
  className?: string;
};

export function Alert({ variant = 'info', children, className }: AlertProps) {
  const { container, icon: Icon } = variantStyles[variant];
  return (
    <div className={cn('flex items-start gap-2.5 rounded-[var(--radius-md)] border px-3 py-2.5 text-[13px]', container, className)}>
      <Icon className="mt-0.5 h-4 w-4 shrink-0" />
      <div>{children}</div>
    </div>
  );
}
