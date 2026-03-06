'use client';

import { ReactNode } from 'react';
import { cn } from '@/lib/cn';

export function PageStack({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('space-y-6 py-6', className)}>{children}</div>;
}

export function PageSection({
  children,
  className,
  variant = 'panel',
}: {
  children: ReactNode;
  className?: string;
  variant?: 'panel' | 'soft';
}) {
  return (
    <section
      className={cn(
        variant === 'panel' ? 'lume-panel' : 'lume-panel-soft',
        'rounded-[var(--radius-2xl)] p-5 md:p-6',
        className,
      )}
    >
      {children}
    </section>
  );
}

export function PageSplit({
  left,
  right,
  className,
}: {
  left: ReactNode;
  right: ReactNode;
  className?: string;
}) {
  return <div className={cn('grid gap-4 xl:grid-cols-[320px_minmax(0,1fr)]', className)}>{left}{right}</div>;
}

