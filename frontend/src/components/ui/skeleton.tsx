import { cn } from '@/lib/cn';

interface SkeletonProps {
  className?: string;
  lines?: number;
  height?: string;
  rounded?: string;
}

export function Skeleton({ className, height = 'h-4', rounded = 'rounded-md' }: SkeletonProps) {
  return (
    <div
      className={cn('shimmer', height, rounded, className)}
      role="status"
      aria-label="Carregando..."
    />
  );
}

export function SkeletonText({ lines = 3, className }: { lines?: number; className?: string }) {
  return (
    <div className={cn('space-y-2', className)}>
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton
          key={i}
          className={i === lines - 1 ? 'w-3/4' : 'w-full'}
        />
      ))}
    </div>
  );
}

export function SkeletonCard({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        'rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-1)] p-4 space-y-3',
        className
      )}
    >
      <div className="flex items-center gap-3">
        <Skeleton className="h-9 w-9 rounded-full" />
        <div className="flex-1 space-y-1.5">
          <Skeleton className="h-3.5 w-1/3" />
          <Skeleton className="h-3 w-1/4" />
        </div>
      </div>
      <SkeletonText lines={2} />
    </div>
  );
}
