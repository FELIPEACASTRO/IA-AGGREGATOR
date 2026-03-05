import { cn } from '@/lib/cn';

interface ProgressBarProps {
  value: number; // 0–100
  className?: string;
  label?: string;
  showLabel?: boolean;
  size?: 'sm' | 'md' | 'lg';
  warnThreshold?: number;
  dangerThreshold?: number;
  colorThreshold?: { warn: number; danger: number };
}

export function ProgressBar({
  value,
  className,
  label,
  showLabel = false,
  size = 'md',
  warnThreshold,
  dangerThreshold,
  colorThreshold = { warn: warnThreshold ?? 80, danger: dangerThreshold ?? 95 },
}: ProgressBarProps) {
  const clamped = Math.min(100, Math.max(0, value));
  const isDanger = clamped >= colorThreshold.danger;
  const isWarn = !isDanger && clamped >= colorThreshold.warn;

  const barColor = isDanger
    ? 'bg-[var(--destructive)]'
    : isWarn
    ? 'bg-[var(--warning)]'
    : 'bg-[var(--brand-primary)]';

  const trackH = size === 'sm' ? 'h-1' : size === 'lg' ? 'h-2.5' : 'h-1.5';

  return (
    <div className={cn('w-full', className)}>
      {(label || showLabel) && (
        <div className="mb-1 flex items-center justify-between text-[var(--text-xs)] text-[var(--muted-foreground)]">
          {label && <span>{label}</span>}
          {showLabel && <span className={isDanger ? 'text-[var(--destructive)]' : isWarn ? 'text-[var(--warning)]' : ''}>{clamped.toFixed(0)}%</span>}
        </div>
      )}
      <div
        className={cn('w-full overflow-hidden rounded-[var(--radius-pill)] bg-[var(--surface-2)]', trackH)}
        role="progressbar"
        aria-valuenow={clamped}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={label || 'Progresso'}
      >
        <div
          className={cn('h-full rounded-[var(--radius-pill)] transition-[width]', barColor, 'duration-[var(--dur-slow)]')}
          style={{ width: `${clamped}%` }}
        />
      </div>
    </div>
  );
}
