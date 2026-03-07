'use client';

import { cn } from '@/lib/cn';

interface SkeletonProps {
  className?: string;
  height?: string;
}

export function Skeleton({ className, height = 'h-4' }: SkeletonProps) {
  return (
    <div
      className={cn('shimmer rounded-[var(--radius-md)]', height, className)}
      role="status"
      aria-label="Carregando..."
    />
  );
}

export function SkeletonText({ lines = 3, className }: { lines?: number; className?: string }) {
  return (
    <div className={cn('space-y-2', className)}>
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} className={i === lines - 1 ? 'w-3/4' : 'w-full'} />
      ))}
    </div>
  );
}
